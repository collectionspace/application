package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.collectionspace.chain.controller.ChainServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBase extends TestData {
	private static final Logger log=LoggerFactory.getLogger(TestBase.class);
	
	private String cookie;
	
	
	protected void login(ServletTester tester) throws IOException, Exception {
		String test = "{\"userid\":\"test@collectionspace.org\",\"password\":\"testtest\"}";
		HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
		log.info(out.getContent());
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.debug("Got cookie "+cookie);
	}
	protected ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}

	// XXX refactor
	protected HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	protected JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		return out;
	}
	
	protected String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}
	
	protected String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	protected JSONObject getFields(JSONObject in) throws JSONException {
		in=in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}
	
	protected JSONObject createRoleWithPermission(String role, String permname, String permname2) throws Exception{

		/*
        "permissions": [
            {"resourceName": "Acquisition", "permission": "write"},
            {"resourceName": "Loan In", "permission": "read"},
        ],
		 */
		JSONArray permission = new JSONArray();
		JSONObject perm1 = new JSONObject();
		perm1.put("resourceName", permname);
		perm1.put("permission", "read");

		JSONObject perm2 = new JSONObject();
		perm2.put("resourceName", permname2);
		perm2.put("permission", "write");
		permission.put(perm1);
		permission.put(perm2);
		JSONObject roleJSON= new JSONObject(role);
		roleJSON.put("permissions", permission);
		return roleJSON;		
	}
	
	protected JSONObject createUserWithRoles(ServletTester jetty,String user, String roleJSON) throws Exception{

		//create role
		HttpTester out = jettyDo(jetty,"POST","/chain/role/",makeSimpleRequest(roleJSON));
		log.info(out.getContent());
		JSONObject role = new JSONObject(out.getContent()).getJSONObject("fields");
		String role_id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		
		/*
        "role": [
            {"roleName": "Acquisition", "roleId": "write", "active":"active"},
        ],
		 */
		JSONArray roles = new JSONArray();
		JSONObject role1 = new JSONObject();
		role1.put("roleName", role.getString("roleName"));
		role1.put("roleId", role_id);
		role1.put("roleSelected", "true");
		
		roles.put(role1);

		JSONObject userJSON= new JSONObject(user);
		userJSON.put("role", roles);
		return userJSON;		

	}
}
