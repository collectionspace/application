package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSessions {
	private static final Logger log=LoggerFactory.getLogger(TestSessions.class);
	
	// XXX refactor
	private UTF8SafeHttpTester jettyDo(ServletTester tester,String method,String path,String data_str,String session) throws IOException, Exception {
		UTF8SafeHttpTester out=new UTF8SafeHttpTester();
		
		out.request(tester,method,path,data_str,session);
		return out;
	}
	
	// XXX refactor into other copy of this method
	private ServletTester setupJetty() throws Exception {
		BootstrapConfigController config_controller=new BootstrapConfigController(null);
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.go();
		String base=config_controller.getOption("services-url");		
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("storage","service");
		tester.setAttribute("store-url",base+"/cspace-services/");	
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		return tester;
	}
	
	@Test public void testSessions() throws Exception {
		ServletTester jetty=setupJetty();
		// Get a cookie
		UTF8SafeHttpTester response=jettyDo(jetty,"GET","/chain/objects/uispec",null,null);
		assertEquals(200,response.getStatus());
		String cookie=response.getHeader("Set-Cookie");
		assertNotNull(cookie);
		assertTrue(cookie.startsWith("CSPACESESSID="));
		log.info(cookie);
		// Check we don't get a second "set"
		response=jettyDo(jetty,"GET","/chain/objects/uispec",null,cookie);
		assertEquals(200,response.getStatus());
		cookie=response.getHeader("Set-Cookie");
		assertNull(cookie);
	}
}
