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
	
	
	

	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty();
		JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
		JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		HttpTester out = jettyDo(jetty,"POST","/chain/users/",makeRequest(userdata).toString());
		log.info("1::"+out.getContent());
		assertEquals(201,out.getStatus());
	}
	
	
}
