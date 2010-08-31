package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAjaxExpiresHeaders {
	private static final Logger log=LoggerFactory.getLogger(TestAjaxExpiresHeaders.class);
	private static String cookie;
	
	// XXX refactor
	protected InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	// XXX refactor
	private String getResourceString(String name) throws IOException {
		InputStream in=getResource(name);
		return IOUtils.toString(in);
	}

	private static void login(ServletTester tester) throws IOException, Exception {
		HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.debug("Got cookie "+cookie);
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
		login(tester);
		return tester;
	}

	
	private static HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");		
		if(data!=null)
			request.setContent(data);
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	@SuppressWarnings("unchecked")
	@Test public void testNoCacheHeaders() throws Exception {
		ServletTester jetty=setupJetty();

		HttpTester out=jettyDo(jetty,"GET","/chain/find-edit/uispec",null);
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
