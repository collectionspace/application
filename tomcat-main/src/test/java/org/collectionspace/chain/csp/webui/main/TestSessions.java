package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
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
		log.debug(cookie);
		// Check we don't get a second "set"
		response=GETData("/intake/uispec",jetty);
		assertEquals(200,response.getStatus());
		cookie=response.getHeader("Set-Cookie");
		assertNull(cookie);
	}
}
