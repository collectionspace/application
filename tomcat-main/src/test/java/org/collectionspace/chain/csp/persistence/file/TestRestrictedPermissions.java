package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;

import org.collectionspace.chain.csp.persistence.TestBase;
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

	 @After public void destroyUser() throws Exception{

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
	

	
	@Before public void createUsers() throws Exception{

		ServletTester jetty = setupJetty();
		HttpTester out;
//READ
		out = POSTData("/role",roleRead,jetty);
		String roler_id = out.getHeader("Location");
		deleteme.add(roler_id);
		JSONObject userrdata = createUserWithRolesById(jetty,userRead,roler_id); 
		out=POSTData("/users/",makeRequest(userrdata).toString(),jetty);
		String user_r_id = out.getHeader("Location");
		deleteme.add(user_r_id);
//WRITE		
		out = POSTData("/role",roleWrite,jetty);
		String rolew_id = out.getHeader("Location");
		deleteme.add(rolew_id);
		JSONObject userwdata = createUserWithRolesById(jetty,userWrite,rolew_id); 
		out=POSTData("/users/",makeRequest(userwdata).toString(),jetty);
		String user_w_id = out.getHeader("Location");
		deleteme.add(user_w_id);
//NONE
		out = POSTData("/role",roleNone,jetty);
		String rolen_id = out.getHeader("Location");
		deleteme.add(rolen_id);
		JSONObject userndata = createUserWithRolesById(jetty,userNone,rolen_id); 
		out=POSTData("/users/",makeRequest(userndata).toString(),jetty);
		String user_n_id = out.getHeader("Location");
		deleteme.add(user_n_id);
		
	}

	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty(getRestrictedUser(userWrite));
		HttpTester out;
		out = GETData("/loginstatus",jetty);
		/* user doesn't seem to exist long enough to login as in the course of the unit test... 
		 *  or else I am doing bad things when creating a user...
		 * */
	//	JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
	//	JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		//HttpTester out = POSTData("/users/",makeRequest(userdata),jetty);
		log.info("1::"+out.getContent());

		//out = GETData("/objects",jetty);

/*		testLists(jetty, "objects", objectCreate, "items");
		testLists(jetty, "intake", intakeCreate, "items");
		testLists(jetty, "loanin", loaninCreate, "items");
		testLists(jetty, "loanout", loanoutCreate, "items");
		testLists(jetty, "acquisition", acquisitionCreate, "items");
		testLists(jetty, "role", roleCreate, "items");
		testLists(jetty, "movement", movementCreate, "items");
		*/
			//testLists(jetty, "permission", permissionWrite, "items");
	}
	
	
}
