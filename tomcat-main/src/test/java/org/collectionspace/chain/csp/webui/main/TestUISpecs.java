package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.util.json.JSONUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUISpecs extends TestBase {
	private static final Logger log=LoggerFactory.getLogger(TestUISpecs.class);

	private void uispec(ServletTester jetty, String url, String uijson) throws Exception {

		HttpTester response;
		JSONObject generated;
		JSONObject comparison;

		response=jettyDo(jetty,"GET",url,null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString(uijson));
		log.info("BASELINE"+comparison.toString());
		log.info("GENERATED"+generated.toString());
		assertTrue("Failed to create correct uispec for "+url,JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));
		
	}
	
	@Test public void testUISpec() throws Exception {
		ServletTester jetty=setupJetty();

//		uispec(jetty,"/chain/acquisition/uispec","acquisition.uispec");
//		uispec(jetty,"/chain/objects/uispec","collection-object.uispec");
//		uispec(jetty,"/chain/object-tab/uispec","object-tab.uispec");
//		uispec(jetty,"/chain/intake/uispec","intake.uispec");
//		uispec(jetty,"/chain/acquisition/uispec","acquisition.uispec");
//		uispec(jetty,"/chain/loanout/uispec","loanout.uispec");
//		uispec(jetty,"/chain/person/uispec","person.uispec");
//		uispec(jetty,"/chain/organization/uispec","organization-authority.uispec");
//		uispec(jetty,"/chain/loanin/uispec","loanin.uispec");
//		uispec(jetty,"/chain/users/uispec","users.uispec");
//		uispec(jetty,"/chain/role/uispec","roles.uispec");
//		uispec(jetty,"/chain/permission/uispec","permissions.uispec");
//		uispec(jetty,"/chain/permrole/uispec","permroles.uispec");
//		uispec(jetty,"/chain/movement/uispec","movement.uispec");
//		uispec(jetty,"/chain/movement-tab/uispec","movement.uispec");
//		
		
		uispec(jetty,"/chain/find-edit/uispec","find-edit.uispec");
		
	
	}
}
