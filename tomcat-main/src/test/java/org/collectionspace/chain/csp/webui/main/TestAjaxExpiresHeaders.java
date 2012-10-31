/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAjaxExpiresHeaders {
	private static final Logger log=LoggerFactory.getLogger(TestAjaxExpiresHeaders.class);
	private static String cookie;
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			log.error("TestAjaxExpiresHeaders: Could not set up jetty! " + ex.getLocalizedMessage());
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}

	/* This test no longer makes sense, as we now want to cache most static resources. 
	 * OTOH, developers may override this, and so fixing the test one way or the other will not
	 * work well. Perhaps we should instead create something and verify that it does not
	 * have cache headers. OTOH, we will likely move away from the pragma headers, since
	 * that is no longer a major platform issue. At some point, we can revisit.
	 */
	@SuppressWarnings("unchecked")
	@Test public void testNoCacheHeaders() throws Exception {
		/*
		HttpTester out = tester.GETData("/intake/uispec",jetty);
		assertEquals("no-cache",out.getHeader("pragma"));
		String last_modified=out.getHeader("Last-Modified");
		assertNotNull(last_modified);
		SimpleDateFormat format=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");
		Date when=format.parse(last_modified);
		assertTrue(when.getTime()<new Date().getTime());
		Enumeration cc=out.getHeaderValues("Cache-Control");
		int to_get=0;
		while(cc.hasMoreElements()) {
			String value=(String)cc.nextElement();
			if("post-check=0, pre-check=0".equals(value)) to_get|=1;
			else if("no-store, no-cache, must-revalidate".equals(value)) to_get|=2;
			else to_get|=4;
		}
		assertEquals(3,to_get);
	 */
	}
}
