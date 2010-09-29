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
	private static final Logger log = LoggerFactory
			.getLogger(TestUISpecs.class);

	private void uispec(ServletTester jetty, String url, String uijson)
			throws Exception {

		HttpTester response;
		JSONObject generated;
		JSONObject comparison;

		response = GETData(url, jetty);

		generated = new JSONObject(response.getContent());
		comparison = new JSONObject(getResourceString(uijson));
		log.info("BASELINE" + comparison.toString());
		log.info("GENERATED" + generated.toString());
		assertTrue("Failed to create correct uispec for " + url, JSONUtils
				.checkJSONEquivOrEmptyStringKey(generated, comparison));

	}

	@Test
	public void testUISpec() throws Exception {
		ServletTester jetty = setupJetty();

		// uispec(jetty,"/acquisition/uispec","acquisition.uispec");
		// uispec(jetty,"/movement/generator?quantity=10","acquisition.uispec");
		// uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		// uispec(jetty,"/person/generator?quantity=10","acquisition.uispec");
		// uispec(jetty,"/objects/uispec","collection-object.uispec");
		 uispec(jetty,"/role/uischema","collection-object.uischema");
		// uispec(jetty,"/object-tab/uispec","object-tab.uispec");
		// uispec(jetty,"/intake/uispec","intake.uispec");
		// uispec(jetty,"/acquisition/uispec","acquisition.uispec");
		// uispec(jetty,"/loanout/uispec","loanout.uispec");
		// uispec(jetty,"/person/uispec","person.uispec");
		// uispec(jetty,"/organization/uispec","organization-authority.uispec");
		// uispec(jetty,"/loanin/uispec","loanin.uispec");
		// uispec(jetty,"/users/uispec","users.uispec");
		// uispec(jetty,"/role/uispec","roles.uispec");
		// uispec(jetty,"/permission/uispec","permissions.uispec");
		// uispec(jetty,"/permrole/uispec","permroles.uispec");
		// uispec(jetty,"/movement/uispec","movement.uispec");
		// uispec(jetty,"/movement-tab/uispec","movement.uispec");
		//		

		uispec(jetty, "/find-edit/uispec", "find-edit.uispec");

	}
}
