/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.TenantServlet;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.collectionspace.chain.util.json.JSONUtils;
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
	private static final Logger log = LoggerFactory.getLogger(TestBase.class);

	private String defaulttenant="core";
	public  String cookie;
	
	public static String AUTHS_INIT_PATH = "/authorities/initialise";
	public static String VOCABS_INIT_PATH = "/authorities/vocab/initialize";
	public static String MAIN_PERSON_INSTANCE_PATH = "/vocabularies/person";
	public static String SECOND_PERSON_INSTANCE_PATH = "/vocabularies/ulan_pa";
	public static String MAIN_ORG_INSTANCE_PATH = "/vocabularies/organization";
	public static String SECOND_ORG_INSTANCE_PATH = "/vocabularies/ulan_oa";

	
	public  String getCurrentYear() {
		Calendar cal = GregorianCalendar.getInstance();
                int year = cal.get(Calendar.YEAR);
		return Integer.toString(year);
	}
	public  void login(String tenant, ServletTester tester) throws IOException,
			Exception {
		JSONObject user = getDefaultUser(tester);
		login(tenant, tester, user, false);
	}

	public  void login(String tenant, ServletTester tester, Boolean isUTF8)
			throws IOException, Exception {
		JSONObject user = getDefaultUser(tester);
		login(tenant, tester, user, isUTF8);
	}

	public  void login(String tenant, ServletTester tester, JSONObject user)
			throws IOException, Exception {
		login(tenant, tester, user, false);
	}
    
	public  void login(ServletTester tester) throws IOException,
			Exception {
		JSONObject user = getDefaultUser(tester);
		login(defaulttenant, tester, user, false);
	}

	public  void login(ServletTester tester, Boolean isUTF8)
			throws IOException, Exception {
		JSONObject user = getDefaultUser(tester);
		login(defaulttenant, tester, user, isUTF8);
	}

	public  void login(ServletTester tester, JSONObject user)
			throws IOException, Exception {
		login(defaulttenant, tester, user, false);
	}

	public  void login(String tenant, ServletTester tester, JSONObject user,
			Boolean isUTF8) throws IOException, Exception {
		String test = user.toString();
		if (isUTF8) {
			UTF8SafeHttpTester out = jettyDoUTF8(tester, "POST",
					"/tenant/"+tenant+"/login/", test);
			assertEquals(303, out.getStatus());
			cookie = out.getHeader("Set-Cookie");
		} else {
			HttpTester out = jettyDo(tester, "POST", "/tenant/"+tenant+"/login/", test);
			log.trace(out.getContent());
			assertEquals(303, out.getStatus());
			cookie = out.getHeader("Set-Cookie");
		}

		log.debug("Got cookie " + cookie);
		cookie=cookie.replaceAll(";.*$","");
	}

	public  ServletTester setupJetty() throws Exception {
		return setupJetty(defaulttenant, null, false);
	}


	public  ServletTester setupJetty(String tenant, JSONObject user, String configfile) throws Exception {
		return setupJetty( tenant, user, false, configfile);
	}

	public  ServletTester setupJetty(String tenant, JSONObject user) throws Exception {
		return setupJetty(tenant, user, false);
	}
	public  ServletTester setupJetty(JSONObject user) throws Exception {
		return setupJetty(null, user, false);
	}

	public  ServletTester setupJetty(String tenant, Boolean isUTF8)
			throws Exception {
		return setupJetty( tenant, null, isUTF8);
	}


	// controller: "test-config-loader2.xml"
	public  ServletTester setupJetty(String tenant, JSONObject user, Boolean isUTF8) throws Exception {
		return setupJetty(tenant, user,isUTF8,"default.xml");
	}
	public  ServletTester setupJetty(String tenant, JSONObject user, Boolean isUTF8, String configfile) throws Exception {
		String base = "";
				
		ServletTester tester = new ServletTester();
		tester.setContextPath("/tenant");
		tester.addServlet(TenantServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("config-filename", configfile);
		tester.start();
		if (user != null) {
			login(tenant, tester, user, isUTF8);
		} else {
			login(tenant, tester, isUTF8);
		}

		return tester;
	}
	public void stopJetty(ServletTester bob) throws Exception{
		bob.stop();
	}

	public InputStream getLocalResource(String name) {
		String path = getClass().getPackage().getName().replaceAll("\\.", "/")
				+ "/" + name;
		
		return Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(path);
	}
	
	public String getResourceString(String name) throws IOException {
		InputStream in = getLocalResource(name);
		return IOUtils.toString(in);
	}

	public  UTF8SafeHttpTester jettyDoUTF8(ServletTester tester,
			String method, String path, String data_str) throws IOException,
			Exception {
		UTF8SafeHttpTester out = new UTF8SafeHttpTester();
		out.request(tester, method, path, data_str, cookie);
		return out;
	}

	public  UTF8SafeHttpTester jettyDoData(ServletTester tester,
			String method, String path, String filename, String content_type,byte[] data) throws IOException,
			Exception {
		UTF8SafeHttpTester out = new UTF8SafeHttpTester();
		byte[] head=("------JettyTester\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+filename+"\"\r\nContent-Type: "+content_type+"\r\n\r\n").getBytes("UTF-8");
		byte[] tail="\r\n------JettyTester--\r\n".getBytes("UTF-8");
		byte[] msg = new byte[head.length+data.length+tail.length];
		System.arraycopy(head,0,msg,0,head.length);
		System.arraycopy(data,0,msg,head.length,data.length);
		System.arraycopy(tail,0,msg,head.length+data.length,tail.length);
		
		out.request_binary(tester, method, path, msg, cookie,"multipart/form-data; boundary=----JettyTester");
		return out;
	}
	
	public  HttpTester jettyDo(ServletTester tester, String method,
			String path, String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host", "tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");
		if (cookie != null)
			request.addHeader(HttpHeaders.COOKIE, cookie);
		if (data != null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}

	public JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out = new JSONObject();
		out.put("fields", fields);
		return out;
	}

	public JSONObject makeRequest(JSONObject fields, JSONObject[] relations)
			throws JSONException {
		JSONObject out = new JSONObject();
		out.put("fields", fields);
		if (relations != null) {
			JSONArray r = new JSONArray();
			for (JSONObject s : relations)
				r.put(s);
			out.put("relations", r);
		}
		return out;
	}

	public String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}

	public String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	public JSONObject getFields(JSONObject in) throws JSONException {
		in = in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}

	public Boolean testStatus(String type, Integer status) {
		if (type.equals("GET")) {
			return (status == 200);
		} else if (type.equals("POST")) {
			return (status == 201);
		} else if (type.equals("PUT")) {
			return (status == 200);
		} else if (type.equals("DELETE")) {
			return (status == 200);
		} else if (type.equals("GETFAIL")) {
			return (status == 404);
		}
		return false;
	}
	/**
	 * to be used when we are testing permissions and we want things to fail with 403 etc
	 * @param type
	 * @param status
	 * @param testValue
	 * @return
	 */
	public Boolean testStatus(String type, Integer status, Integer testValue) {
		if (type.equals("GET")) {
			return (status == testValue);
		} else if (type.equals("POST")) {
			return (status == testValue);
		} else if (type.equals("PUT")) {
			return (status == testValue);
		} else if (type.equals("DELETE")) {
			return (status == testValue);
		} else if (type.equals("GETFAIL")) {
			return (status == testValue);
		}
		return false;
	}

	/**
	 * package with default tests for success
	 * @param id
	 * @param jetty
	 * @throws IOException
	 * @throws Exception
	 */
	public void DELETEData(String id, ServletTester jetty) throws IOException, Exception {

		HttpTester out=jettyDo(jetty,"DELETE","/tenant/"+defaulttenant+id,null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a DELETE url: /tenant/"+defaulttenant+id +"/n"+out.getContent(),testStatus("DELETE",status));
		log.debug(id+":"+out.getContent());
		
		//out=jettyDo(jetty,"GET","/tenant/html"+id,null);
		//assertTrue(testStatus("GETFAIL",out.getStatus()));
	}
	/**
	 * package with default tests for success
	 * @param id
	 * @param jetty
	 * @throws IOException
	 * @throws Exception
	 */
	public void DELETEData(String id, ServletTester jetty, String data) throws IOException, Exception {

		HttpTester out=jettyDo(jetty,"DELETE","/tenant/"+defaulttenant+id,data);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a DELETE url: /tenant/"+defaulttenant+id +"/n"+out.getContent()+" with data "+data,testStatus("DELETE",status));
		log.debug(id+":"+out.getContent());
		
		//out=jettyDo(jetty,"GET","/tenant/html"+id,null);
		//assertTrue(testStatus("GETFAIL",out.getStatus()));
	}

	/**
	 * package with default status tests for success
	 * @param url
	 * @param data
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester POSTData(String url, String data, ServletTester jetty) throws IOException, Exception{
		HttpTester out = jettyDo(jetty,"POST","/tenant/"+defaulttenant+url,data);
		assertEquals(out.getMethod(),null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a POST url: /tenant/"+defaulttenant+url+" with data: "+data +"/n"+out.getContent(),testStatus("POST",status));
		return out;
	}
	
	public HttpTester POSTData(String url, String data, ServletTester jetty, String statusTest) throws IOException, Exception{
		HttpTester out = jettyDo(jetty,"POST","/tenant/"+defaulttenant+url,data);
		assertEquals(out.getMethod(),null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+":"+statusTest+" was wrong for a POST url: /tenant/"+defaulttenant+url+" with data: "+data +"/n"+out.getContent(),testStatus(statusTest,status));
		return out;
	}

	public UTF8SafeHttpTester POSTBinaryData(String url, byte[] data, ServletTester jetty) throws IOException, Exception{
		UTF8SafeHttpTester out = jettyDoData(jetty,"POST","/tenant/"+defaulttenant+url,"1.jpeg","image/jpeg",data);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a POST url: /tenant/"+defaulttenant+url+" with data: "+data +"/n"+out.getContent(),testStatus("PUT",status));
		return out;
	}
	/**
	 * package with default status tests for success
	 * @param url
	 * @param json
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester POSTData(String url, JSONObject json, ServletTester jetty) throws IOException, Exception{
		return POSTData(url,json.toString(),jetty);
	}
	public HttpTester POSTData(String url, JSONObject json, ServletTester jetty, String testStatus) throws IOException, Exception{
		return POSTData(url,json.toString(),jetty,testStatus);
	}
	
	
	/**
	 * package with default status tests for success
	 * @param url
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester GETData(String url, ServletTester jetty) throws IOException, Exception{
	//	return GETData(url,null,jetty);
		HttpTester out=jettyDo(jetty,"GET","/tenant/"+defaulttenant+url,null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a GET url: /tenant/"+defaulttenant+url+" /n"+out.getContent(),testStatus("GET",status));
		log.debug(url+":"+out.getContent());
		return out;
	
	}
	
	public HttpTester GETData(String url, ServletTester jetty, Integer testStatus) throws IOException, Exception{
		HttpTester out=jettyDo(jetty,"GET","/tenant/"+defaulttenant+url,null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a GET where we were expecting "+ Integer.toString(testStatus)+" url : /tenant/"+defaulttenant+url+" /n"+out.getContent(),(Integer.toString(testStatus).equals(Integer.toString(status))));
		log.debug(url+":"+out.getContent());
		return out;
	}

	public UTF8SafeHttpTester GETBinaryData(String url, ServletTester jetty, Integer testStatus) throws IOException, Exception{
		url = url.replace("/chain/", "/tenant/"+defaulttenant+"/");
		if(url.contains("/tenant/"+defaulttenant+"/"))
			url = url.substring(url.indexOf("/tenant/"+defaulttenant+"/"));
		else if(url.contains("http"))
			url = url;
		else
			url = "/tenant/"+defaulttenant+"" + url;
		
		log.info(url);
		UTF8SafeHttpTester out=jettyDoUTF8(jetty,"GET",url,null);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a GET where we were expecting "+ Integer.toString(testStatus)+" url : "+url+" /n"+out.getContent(),(Integer.toString(testStatus).equals(Integer.toString(status))));
		log.debug(url+":"+out.getContent());
		return out;
	}

	
	/**
	 * package with default status tests for success
	 * @param url
	 * @param params
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester GETData(String url, String params, ServletTester jetty) throws IOException, Exception{
		HttpTester out=jettyDo(jetty,"GET","/tenant/"+defaulttenant+""+url,params);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a GET url: /tenant/"+defaulttenant+""+url+" "+params +"/n"+out.getContent(),testStatus("GET",status));
		log.debug(url+":"+out.getContent());
		return out;
	}
	
	/**
	 * package with default status tests for success
	 * @param url
	 * @param data
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester PUTData(String url, String data, ServletTester jetty ) throws IOException, Exception{

		HttpTester out=jettyDo(jetty,"PUT","/tenant/"+defaulttenant+""+url,data);
		Integer status = getStatus(out.getContent(),  out.getStatus());
		assertTrue("Status "+Integer.toString(status)+" was wrong for a PUT url: /tenant/"+defaulttenant+""+url+" "+data +"/n"+out.getContent(),testStatus("PUT",status));
		log.debug(url+":"+out.getContent());
		return out;
	}
	
	public Integer getStatus(String content, Integer status){

		if (status <= 400) {
			try{
				JSONObject test = new JSONObject(content);
				if (test.has("status")) {
					status = Integer.parseInt(test.getString("status"));
				}
			}
			catch(Exception ex){
				//do nothing - obviously isn't what we hoped for
			}
		}
		return status;
	}
	/**
	 * package with default status tests for success
	 * @param url
	 * @param json
	 * @param jetty
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public HttpTester PUTData(String url, JSONObject json, ServletTester jetty ) throws IOException, Exception{
		return PUTData(url,json.toString(),jetty);
	}
	
	
	
	public JSONObject createRoleWithPermission(String role, String permname,
			String permname2) throws Exception {

		/*
		 * "permissions": [ {"resourceName": "Acquisition", "permission":
		 * "write"}, {"resourceName": "Loan In", "permission": "read"}, ],
		 */
		JSONObject perm11 = new JSONObject();
		perm11.put("resourceName", "permission");
		perm11.put("permission", "delete");
		JSONObject perm12 = new JSONObject();
		perm12.put("resourceName", "authorization/permissions/permroles");
		perm12.put("permission", "delete");
		JSONObject perm13 = new JSONObject();
		perm13.put("resourceName", "userrole");
		perm13.put("permission", "delete");
		JSONObject perm14 = new JSONObject();
		perm14.put("resourceName", "permrole");
		perm14.put("permission", "delete");
		JSONObject perm15 = new JSONObject();
		perm15.put("resourceName", "authorization/roles/accountroles");
		perm15.put("permission", "delete");
		
		JSONArray permission = new JSONArray();
		JSONObject perm1 = new JSONObject();
		perm1.put("resourceName", permname);
		perm1.put("permission", "read");

		JSONObject perm2 = new JSONObject();
		perm2.put("resourceName", permname2);
		perm2.put("permission", "write");
		permission.put(perm1);
		permission.put(perm2);
		permission.put(perm11);
		permission.put(perm12);
		permission.put(perm13);
		permission.put(perm14);
		permission.put(perm15);
		JSONObject roleJSON = new JSONObject(role);
		roleJSON.put("permissions", permission);
		return roleJSON;
	}

	public JSONObject createUserWithRolesById(ServletTester jetty,
			String user, String roleId) throws Exception {
		// create role
		log.info("createUserWithRolesById "+user+":"+roleId);
		HttpTester out = GETData(roleId, jetty);
		JSONObject role = new JSONObject(out.getContent())
				.getJSONObject("fields");

		JSONArray roles = new JSONArray();
		JSONObject role1 = new JSONObject();
		role1.put("roleName", role.getString("roleName"));
		role1.put("roleId", roleId.split("/")[2]);
		role1.put("roleSelected", "true");

		roles.put(role1);

		JSONObject userJSON = new JSONObject(user);
		userJSON.put("role", roles);
		return userJSON;

	}

	public JSONObject createUserWithRoles(ServletTester jetty, String user,
			String roleJSON) throws Exception {
		// create role
		HttpTester out = POSTData("/role/",
				makeSimpleRequest(roleJSON), jetty);
		JSONObject role = new JSONObject(out.getContent())
				.getJSONObject("fields");
		String role_id = out.getHeader("Location");

		return createUserWithRolesById(jetty, user, role_id);

	}

	// generic test post/get/put delete
	public void testPostGetDelete(ServletTester jetty, String uipath,
			String data, String testfield) throws Exception {
		HttpTester out;
		// Create
		out = POSTData(uipath, makeSimpleRequest(data),jetty);
		log.trace(out.getContent());
		String id = out.getHeader("Location");
		// Retrieve
		out = jettyDo(jetty, "GET", "/tenant/"+defaulttenant+"" + id, null);
		log.trace(out.getContent());
		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(data);
		
		assertEquals("testPostGetDelete("+uipath+") Tested field not the same: "+testfield, 
				one.get(testfield).toString(), two.get(testfield).toString());

		// change
		if (!uipath.contains("permission")) {
			two.put(testfield, "newvalue");
			log.debug(id);
			log.debug(makeRequest(two).toString());
			out = PUTData(id, makeRequest(two), jetty);
			JSONObject oneA = new JSONObject(getFields(out.getContent()));
			assertEquals("testPostGetDelete("+uipath+") Updated value not the same on testfield: "+testfield, oneA.get(testfield).toString(), "newvalue");
		}

		// Delete
		DELETEData(id, jetty);

	}

	// UI specs
	public void testUIspec(ServletTester jetty, String url, String uijson) throws Exception {
		log.info("testUISpec(" + url + ", " + uijson + ")");
		
		String errMsg = String.format("Failed to create correct uispec for '%s' compared with file '%s'",
				url, uijson);
		JSONObject generated = new JSONObject("{}");
		JSONObject comparison = new JSONObject("{}");
		boolean success = false;

		try {
			HttpTester response = GETData(url, jetty);

			generated = new JSONObject(response.getContent());
			comparison = new JSONObject(getResourceString(uijson));
			xxxfixOptions(generated);
			xxxfixOptions(comparison);

			// You can use these, Chris, to write stuff out if the spec has
			// changed to alter the test file -- dan
			// hendecasyllabic:tmp csm22$ cat gschema.out | pbcopy

			success = JSONUtils.checkJSONEquivOrEmptyStringKey(generated, comparison);
		} catch (Exception e) {
			log.error(errMsg, e);
			errMsg = String.format("%s. %s", errMsg, e.getMessage()); // Append the exception message to our default error message.
		}

		if (!success) {
			log.error("testUIspec(" + uijson + ") BASELINE from file" + comparison.toString());
			log.error("testUIspec(" + url + ") GENERATED from url" + generated.toString());
		}

		assertTrue(errMsg, success);
	}

	public JSONArray xxxsorted(JSONArray in) throws Exception {
		JSONArray out=new JSONArray();
		Object[] v=new Object[in.length()];
		for(int i=0;i<v.length;i++)
			v[i]=in.get(i);
		try{
			Arrays.sort(v);
		}
		catch(Exception ex){
			log.debug("xxxsorted: Unexpected exception: "+ex.getLocalizedMessage());
		}
		for(int i=0;i<v.length;i++)
			out.put(v[i]);
		return out;
	}
	/* XXX at the moment options are returned unsorted from the service layer, so we need to sort them
	 */
	public void xxxfixOptions_a(JSONArray v) throws Exception {
		for(int i=0;i<v.length();i++) {
			Object x=v.get(i);
			if(x instanceof JSONObject)
				xxxfixOptions((JSONObject)x);
			else if(x instanceof JSONArray){
				v.put(i,xxxsorted((JSONArray)x));
				xxxfixOptions_a((JSONArray)x);
			}
		}
	}
	
	public void xxxfixOptions(JSONObject in) throws Exception {
		if(in.has("optionnames"))
			in.put("optionnames",xxxsorted(in.getJSONArray("optionnames")));
		if(in.has("optionlist"))
			in.put("optionlist",xxxsorted(in.getJSONArray("optionlist")));
		Iterator t = in.keys();
		while(t.hasNext()) {
			String k = (String)t.next();
			Object v = in.get(k);
			if(v!= null) {
				if(v instanceof JSONObject)
					xxxfixOptions((JSONObject)v);
				else if(v instanceof JSONArray) {
					in.put(k,xxxsorted((JSONArray)v));
					xxxfixOptions_a((JSONArray)v);
				}
			}
		}
	}
	
	// generic Lists
	public void testLists(ServletTester jetty, String objtype, String data,
			String itemmarker) throws Exception {

		HttpTester out1 = POSTData(objtype,makeSimpleRequest(data), jetty);

		/* get all objects */
		// pagination?
		HttpTester out;
		int pgSz = 100;
		int pgNum = 0;
		boolean exists = false;
		boolean end = false;
		// Page through looking for this id
		do {
			out = GETData(objtype
					+ "search?pageNum=" + pgNum + "&pageSize=" + pgSz, jetty);
			assertEquals(200, out.getStatus());

			/* create list of files */

			JSONObject result = new JSONObject(out.getContent());
			JSONArray items = result.getJSONArray(itemmarker);
			Set<String> files = new HashSet<String>();
			if (items.length() > 0) {
				for (int i = 0; i < items.length(); i++) {
					files.add( objtype + items.getJSONObject(i).getString("csid"));
				}
			} else {
				end = true;
			}

			exists = files.contains(out1.getHeader("Location"));
			pgNum++;
		} while (!end && !exists);

		assertTrue(exists);

		/* clean up */
		DELETEData(out1.getHeader("Location"),jetty);
	}
	public HttpTester createUser(ServletTester jetty, String JSONfile) throws IOException, JSONException, Exception{

		HttpTester out;
		JSONObject u1=new JSONObject(JSONfile);
		String userId = u1.getString("userId");
		JSONObject test = new JSONObject();
		test.put("email", userId);

		/* delete user if already exists */
		out = GETData("/users/search",test.toString(),jetty);
		String itemmarker = "items";
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray(itemmarker);
		if(items.length()>0){
			for(int i=0;i<items.length();i++){
				JSONObject user = items.getJSONObject(i);
				if(user.getString("email").equals(userId)){
					//delete record
					String csid = user.getString("csid");
					log.debug("DELETE + "+csid);
					DELETEData("/users/"+csid,jetty);
				}
			}
		}
		
		// Create a User
		out = POSTData("/users/",makeSimpleRequest(JSONfile),jetty);
		return out;
	}

	@Test public void test(){
		assertTrue(true);
	}

}
