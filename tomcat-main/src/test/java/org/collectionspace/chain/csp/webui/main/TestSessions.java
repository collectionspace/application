/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSessions{
	private static final Logger log=LoggerFactory.getLogger(TestSessions.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}
	
	@Test public void testSessions() throws Exception {
		// Get a cookie
		HttpTester response=tester.GETData("/intake/uispec",jetty);
		//cookie=response.getHeader("Set-Cookie");
		assertNotNull(tester.cookie);
		assertTrue(tester.cookie.startsWith("CSPACESESSID="));
		log.info(tester.cookie);
		// Check we don't get a second "set"
		response=tester.GETData("/intake/uispec",jetty);
		assertEquals(200,response.getStatus());
		String cookie2=response.getHeader("Set-Cookie");
		log.info(cookie2);
		log.info(tester.cookie);
		//assertNull(cookie);
	}
}
