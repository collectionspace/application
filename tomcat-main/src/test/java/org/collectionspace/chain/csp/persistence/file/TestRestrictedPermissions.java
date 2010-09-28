package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRestrictedPermissions extends TestBase{
	private static final Logger log=LoggerFactory.getLogger(TestRestrictedPermissions.class);
	
	
	@Test public void whoamI() throws Exception{

		ServletTester jetty = setupJetty();
		HttpTester out;
		out = GETData("/loginstatus",jetty);


		JSONObject rolepermsdata = createRoleWithPermission(roleCreate,"loanin", "loanout"); 
		out=POSTData("/role/",makeRequest(rolepermsdata).toString(),jetty);
		String role_id = out.getHeader("Location");
		
		//create user with roles in payload
		JSONObject userdata = createUserWithRolesById(jetty,user88Create,role_id); 
		out=POSTData("/users/",makeRequest(userdata).toString(),jetty);
		String userid = out.getHeader("Location");

		//delete role
		DELETEData(role_id,jetty);
		
		//delete user
		DELETEData(userid,jetty);
		
	}


	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty();
	//	JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
	//	JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		//HttpTester out = POSTData("/users/",makeRequest(userdata),jetty);
		//log.info("1::"+out.getContent());
	}
	
	
}
