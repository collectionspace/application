/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.csp.schema.Spec;
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

		//log.info("GENERATED" + response.getContent());
		generated = new JSONObject(response.getContent());
		comparison = new JSONObject(getResourceString(uijson));
		log.info("BASELINE" + comparison.toString());
		log.info("GENERATED" + generated.toString());
		assertTrue("Failed to create correct uispec for " + url, JSONUtils
				.checkJSONEquivOrEmptyStringKey(generated, comparison));

	}

	@Test
	public void testUISchema() throws Exception {
		ServletTester jetty = setupJetty();

		uispec(jetty, "/recordlist/uischema", "recordlist.uischema");
		uispec(jetty, "/cataloging/uischema", "collection-object.uischema");
		uispec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
	}

	@Test
	public void testUISpec() throws Exception {
		ServletTester jetty = setupJetty();

		// uispec(jetty,"/movement/generator?quantity=10","acquisition.uispec");
		// uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		// uispec(jetty,"/person/generator?quantity=10","acquisition.uispec");

/*
		uispec(jetty, "/acquisition/uispec", "acquisition.uispec");
		uispec(jetty, "/cataloging/uispec", "collection-object.uispec");
		uispec(jetty, "/intake/uispec", "intake.uispec");
		uispec(jetty, "/loanin/uispec", "loanin.uispec");
		uispec(jetty, "/loanout/uispec", "loanout.uispec");
		uispec(jetty, "/movement/uispec", "movement.uispec");
		uispec(jetty, "/objectexit/uispec", "objectexit.uispec");
		
		uispec(jetty, "/users/uispec", "users.uispec");
		uispec(jetty, "/role/uispec", "roles.uispec");
		uispec(jetty, "/permission/uispec", "permissions.uispec");
		uispec(jetty, "/permrole/uispec", "permroles.uispec");
		

		uispec(jetty, "/person/uispec", "person.uispec");
		uispec(jetty, "/organization/uispec", "organization-authority.uispec");
		

		uispec(jetty, "/cataloging-tab/uispec", "cataloging-tab.uispec");
		uispec(jetty, "/movement-tab/uispec", "movement-tab.uispec");
		//		
*/
		uispec(jetty, "/myCollectionSpace/uispec", "find-edit.uispec");

	}
}
