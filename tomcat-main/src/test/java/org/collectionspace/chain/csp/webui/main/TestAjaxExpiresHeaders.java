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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAjaxExpiresHeaders extends TestBase  {
	private static final Logger log=LoggerFactory.getLogger(TestAjaxExpiresHeaders.class);
	private static String cookie;

	
	@SuppressWarnings("unchecked")
	@Test public void testNoCacheHeaders() throws Exception {
		ServletTester jetty=setupJetty();

		HttpTester out = GETData("/myCollectionSpace/uispec",jetty);
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
	}
}
