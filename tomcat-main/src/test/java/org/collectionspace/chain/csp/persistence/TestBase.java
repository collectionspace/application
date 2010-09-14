package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBase extends TestData {
	private static final Logger log=LoggerFactory.getLogger(TestBase.class);
	
	private static String cookie;
	

	protected  void login(ServletTester tester) throws IOException, Exception {
		JSONObject user = getDefaultUser();
		login(tester, user, false);
	}
	protected  void login(ServletTester tester, Boolean isUTF8) throws IOException, Exception {
		JSONObject user = getDefaultUser();
		login(tester, user, isUTF8);
	}
	protected  void login(ServletTester tester, JSONObject user) throws IOException, Exception {
		login(tester, user, false);
	}

	protected void login(ServletTester tester, JSONObject user, Boolean isUTF8) throws IOException, Exception {
		String test = user.toString();
		if(isUTF8){
			UTF8SafeHttpTester out=jettyDoUTF8(tester,"POST","/chain/login/",test);
			assertEquals(303,out.getStatus());
			cookie=out.getHeader("Set-Cookie");
		}
		else{
			HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
			assertEquals(303,out.getStatus());
			cookie=out.getHeader("Set-Cookie");
		}
		
		log.debug("Got cookie "+cookie);
	}
	
	
	protected  ServletTester setupJetty() throws Exception {
		return setupJetty("test-config-loader2.xml", null,false);
	}
	protected  ServletTester setupJetty(String controller) throws Exception {
		return setupJetty(controller, null,false);
	}
	protected  ServletTester setupJetty(String controller, JSONObject user) throws Exception {
		return setupJetty(controller, user,false);
	}
	protected  ServletTester setupJetty(JSONObject user) throws Exception {
		return setupJetty("test-config-loader2.xml", user,false);
	}

	protected  ServletTester setupJetty(Boolean isUTF8) throws Exception {
		return setupJetty("test-config-loader2.xml", null,isUTF8);
	}
	protected  ServletTester setupJetty(String controller,Boolean isUTF8) throws Exception {
		return setupJetty(controller, null,isUTF8);
	}
	protected  ServletTester setupJetty(JSONObject user,Boolean isUTF8) throws Exception {
		return setupJetty("test-config-loader2.xml", user,isUTF8);
	}


	//controller: "test-config-loader2.xml"
	protected  ServletTester setupJetty(String controller, JSONObject user,Boolean isUTF8) throws Exception {
		String base="";
		if(controller!=null){
			BootstrapConfigController config_controller=new BootstrapConfigController(null);
			config_controller.addSearchSuffix(controller);
			config_controller.go();
			base=config_controller.getOption("services-url");	
		}
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		if(controller!=null){
			tester.setAttribute("storage","service");
			tester.setAttribute("store-url",base+"/cspace-services/");
		}
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		if(user != null){
			login(tester, user, isUTF8);
		}
		else{
			login(tester, isUTF8);
		}
		
		return tester;
	}
	


	protected InputStream getLocalResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	

	protected String getResourceString(String name) throws IOException {
		InputStream in=getLocalResource(name);
		return IOUtils.toString(in);
	}
	
	protected static UTF8SafeHttpTester jettyDoUTF8(ServletTester tester,String method,String path,String data_str) throws IOException, Exception {
		UTF8SafeHttpTester out=new UTF8SafeHttpTester();
		out.request(tester,method,path,data_str,cookie);
		return out;
	}
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
	
	protected JSONObject makeRequest(JSONObject fields,JSONObject[] relations) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		if(relations!=null) {
			JSONArray r=new JSONArray();
			for(JSONObject s : relations)
				r.put(s);
			out.put("relations",r);
		}
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
		assertEquals(201,out.getStatus());
		JSONObject role = new JSONObject(out.getContent()).getJSONObject("fields");
		String role_id=out.getHeader("Location");
		
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
	
	// generic test post/get/put delete
	protected void testPostGetDelete(ServletTester jetty,String uipath, String data, String testfield) throws Exception {
		HttpTester out;
		//Create
		out = jettyDo(jetty,"POST","/chain"+uipath,makeSimpleRequest(data));
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());	
		String id=out.getHeader("Location");	
		//Retrieve
		out=jettyDo(jetty,"GET","/chain"+id,null);

		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(data);
//		log.info(one.toString());
//		log.info(two.toString());
		assertEquals(one.get(testfield).toString(),two.get(testfield).toString());

		//change
		if(!uipath.contains("permission")){
			two.put(testfield, "newvalue");
			out=jettyDo(jetty,"PUT","/chain"+id,makeRequest(two).toString());
			assertEquals(200,out.getStatus());	
			JSONObject oneA = new JSONObject(getFields(out.getContent()));
			assertEquals(oneA.get(testfield).toString(),"newvalue");
		}

		//Delete
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		
	}
	
	// generic Lists
	protected void testLists(ServletTester jetty, String objtype, String data, String itemmarker)  throws Exception{

		HttpTester out1=jettyDo(jetty,"POST","/chain/"+objtype+"/",makeSimpleRequest(data));
		log.info(out1.getContent());
		assertEquals(201, out1.getStatus());


		/* get all objects */
		//pagination?
		HttpTester out;
		int pgSz = 100;
		int pgNum = 0;
		boolean exists = false;
		boolean end = false;
		// Page through looking for this id
		do{
			out=jettyDo(jetty,"GET","/chain/"+objtype+"/search?pageNum="+pgNum+"&pageSize="+pgSz,null);
			log.info(objtype+":"+out.getContent());
			assertEquals(200,out.getStatus());
			
			/* create list of files */

			JSONObject result=new JSONObject(out.getContent());
			JSONArray items=result.getJSONArray(itemmarker);
			Set<String> files=new HashSet<String>();
			if(items.length() > 0){
				for(int i=0;i<items.length();i++){
					files.add("/"+objtype+"/"+items.getJSONObject(i).getString("csid"));
				}
			}else{
				end = true;
			}

			exists = files.contains(out1.getHeader("Location"));
			pgNum++;
		}while(!end && !exists);
		
		assertTrue(exists);
		
		
		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+out1.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
	}
	
	@Test public void test(){
		assertTrue(true);
	}
}
