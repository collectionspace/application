/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.TenantServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSessions extends TestBase{
	private static final Logger log=LoggerFactory.getLogger(TestSessions.class);
	
	@Test public void testSessions() throws Exception {
		ServletTester jetty=setupJetty();
		// Get a cookie
		HttpTester response=GETData("/intake/uispec",jetty);
		//cookie=response.getHeader("Set-Cookie");
		assertNotNull(cookie);
		assertTrue(cookie.startsWith("CSPACESESSID="));
		log.info(cookie);
		// Check we don't get a second "set"
		response=GETData("/intake/uispec",jetty);
		assertEquals(200,response.getStatus());
		String cookie2=response.getHeader("Set-Cookie");
		log.info(cookie2);
		log.info(cookie);
		//assertNull(cookie);
	}
}
