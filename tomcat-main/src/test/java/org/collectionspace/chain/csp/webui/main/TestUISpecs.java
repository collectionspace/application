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
import org.collectionspace.chain.util.json.JSONUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUISpecs {
	private static final Logger log=LoggerFactory.getLogger(TestUISpecs.class);
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
	private void uispec(ServletTester jetty, String url, String uijson) throws Exception {

		HttpTester response;
		JSONObject generated;
		JSONObject comparison;

		response=jettyDo(jetty,"GET",url,null);
		log.info(response.getContent());
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString(uijson));
		log.info(response.getContent());
		log.info(comparison.toString());
		log.info(generated.toString());
		assertTrue("Failed to create correct uispec for "+url,JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));
		
	}
	
	@Test public void testUISpec() throws Exception {
		ServletTester jetty=setupJetty();

/*		uispec(jetty,"/chain/acquisition/uispec","acquisition.uispec");
		uispec(jetty,"/chain/objects/uispec","collection-object.uispec");
		uispec(jetty,"/chain/object-tab/uispec","object-tab.uispec");
		uispec(jetty,"/chain/intake/uispec","intake.uispec");
		uispec(jetty,"/chain/acquisition/uispec","acquisition.uispec");
		uispec(jetty,"/chain/loanout/uispec","loanout.uispec");
		uispec(jetty,"/chain/person/uispec","person.uispec");
		uispec(jetty,"/chain/organization/uispec","organization-authority.uispec");
		uispec(jetty,"/chain/loanin/uispec","loanin.uispec");
		uispec(jetty,"/chain/users/uispec","users.uispec");
		uispec(jetty,"/chain/role/uispec","roles.uispec");
		uispec(jetty,"/chain/permission/uispec","permissions.uispec");
		uispec(jetty,"/chain/permrole/uispec","permroles.uispec");
		uispec(jetty,"/chain/movement/uispec","movement.uispec");
		uispec(jetty,"/chain/movement-tab/uispec","movement.uispec");
		*/
		
		uispec(jetty,"/chain/find-edit/uispec","find-edit.uispec");
		
	
	}
}
