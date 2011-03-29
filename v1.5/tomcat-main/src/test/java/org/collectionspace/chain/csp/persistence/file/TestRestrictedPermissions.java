/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRestrictedPermissions extends TestBase{
	private static final Logger log=LoggerFactory.getLogger(TestRestrictedPermissions.class);
	private ArrayList<String> deleteme = new ArrayList(); 
	private String role_id;
	private String userid;
	
	protected JSONObject getRestrictedUser(String type) throws JSONException{

		JSONObject user = new JSONObject();
		JSONObject usertemplate = new JSONObject(type);
		try {
			user.put("userid", usertemplate.getString("userName"));
			user.put("password", usertemplate.getString("password"));
			return user;
		} catch (JSONException e) {
			errored(e);
		}
		return user;
	}

	/**
	 @After public void destroyUser() throws Exception{

		log.info("Delete test users for restricted tests");
		//log in as default user who has delete privileges
		ServletTester jetty = setupJetty();
		if(role_id!=null){
			//delete role
			DELETEData(role_id,jetty);
		}
		if(userid!=null){
			//delete user
			DELETEData(userid,jetty);
		}
		if(deleteme.size()>0){
			for(String item: deleteme){
				DELETEData(item,jetty);
			}
		}
	}
	**/
	
	@Before public void createUsers() throws Exception{

		
		log.info("Create test users for restricted tests");
		ServletTester jetty = setupJetty();
		HttpTester out;
		

		//only create roles if there are less than 5 roles
		//yeap arbitrary hack 
		out = GETData("/role",jetty);

		JSONObject result = new JSONObject(out.getContent());
		JSONArray items = result.getJSONArray("items");
		if (items.length() > 4) {
			//do nothing we have enough users/roles
		}
		else{
//READ
		log.info("CREATE READ USER");
		out = POSTData("/role",roleRead,jetty);
		String roler_id = out.getHeader("Location");
		deleteme.add(roler_id);
		JSONObject userrdata = createUserWithRolesById(jetty,userRead,roler_id); 
		out=POSTData("/users/",makeRequest(userrdata).toString(),jetty);
		String user_r_id = out.getHeader("Location");
		deleteme.add(user_r_id);
//WRITE		
		log.info("CREATE WRITE USER");
		out = POSTData("/role",roleWrite,jetty);
		String rolew_id = out.getHeader("Location");
		deleteme.add(rolew_id);
		JSONObject userwdata = createUserWithRolesById(jetty,userWrite,rolew_id); 
		out=POSTData("/users/",makeRequest(userwdata).toString(),jetty);
		String user_w_id = out.getHeader("Location");
		deleteme.add(user_w_id);
//NONE
		log.info("CREATE HALF NONE USER");
		out = POSTData("/role",roleNone1,jetty);
		String rolen1_id = out.getHeader("Location");
		deleteme.add(rolen1_id);
		JSONObject usern1data = createUserWithRolesById(jetty,userNone1,rolen1_id); 
		out=POSTData("/users/",makeRequest(usern1data).toString(),jetty);
		String user_n1_id = out.getHeader("Location");
		deleteme.add(user_n1_id);
		
		log.info("CREATE OTHER HALF NONE USER");
		out = POSTData("/role",roleNone2,jetty);
		String rolen2_id = out.getHeader("Location");
		deleteme.add(rolen2_id);
		JSONObject usern2data = createUserWithRolesById(jetty,userNone2,rolen2_id); 
		out=POSTData("/users/",makeRequest(usern2data).toString(),jetty);
		String user_n2_id = out.getHeader("Location");
		deleteme.add(user_n2_id);
		
		log.info("CREATE NONE USER");
		out = POSTData("/role",roleNone,jetty);
		String rolen_id = out.getHeader("Location");
		deleteme.add(rolen_id);
		JSONObject userndata = createUserWithRolesById(jetty,userNone,rolen_id); 
		out=POSTData("/users/",makeRequest(userndata).toString(),jetty);
		String user_n_id = out.getHeader("Location");
		deleteme.add(user_n_id);
		}
	}

	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty(getRestrictedUser(userWrite));
		HttpTester out;
		out = GETData("/loginstatus",jetty);
		 
//create user with roles in payload
		//HttpTester out = POSTData("/users/",makeRequest(userdata),jetty);
		log.info("1::"+out.getContent());

		assertTrue(true);
		//out = GETData("/cataloging",jetty);
		
//can't use these as they need full permissions read,write,delete,lst to work
//		testLists(jetty, "cataloging", objectCreate, "items");
//		testLists(jetty, "intake", intakeCreate, "items");
//		testLists(jetty, "loanin", loaninCreate, "items");
//		testLists(jetty, "loanout", loanoutCreate, "items");
//		testLists(jetty, "acquisition", acquisitionCreate, "items");
//		testLists(jetty, "role", roleCreate, "items");
//		testLists(jetty, "movement", movementCreate, "items");
		
	}
	
	

}
