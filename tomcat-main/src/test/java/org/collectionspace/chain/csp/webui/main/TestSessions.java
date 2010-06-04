package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSessions {
	private static final Logger log=LoggerFactory.getLogger(TestSessions.class);

	String cookie=null;
	
	// XXX refactor
	private UTF8SafeHttpTester jettyDo(ServletTester tester,String method,String path,String data_str,String session) throws IOException, Exception {
		UTF8SafeHttpTester out=new UTF8SafeHttpTester();
		
		out.request(tester,method,path,data_str,session);
		return out;
	}

	// XXX refactor into other copy of this method
	private ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		//tester.setAttribute("storage","service");
		// tester.setAttribute("store-url",base+"/cspace-services/");	
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}
	
	// XXX refactor
	private HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	private void login(ServletTester tester) throws IOException, Exception {
		//HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		String test = "{\"userid\":\"test@collectionspace.org\",\"password\":\"testtest\"}";
		HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.info("Got cookie "+cookie);
	}
	
	
	@Test public void testSessions() throws Exception {
		ServletTester jetty=setupJetty();
		// Get a cookie
		UTF8SafeHttpTester response=jettyDo(jetty,"GET","/chain/objects/uispec",null,null);
		assertEquals(200,response.getStatus());
		//cookie=response.getHeader("Set-Cookie");
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
