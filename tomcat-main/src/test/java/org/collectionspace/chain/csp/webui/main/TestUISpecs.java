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
		log.info(base);
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		return tester;
	}
	
	private HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");		
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	@Test public void testUISpec() throws Exception {
		ServletTester jetty=setupJetty();
		// Collection-Object
		HttpTester response=jettyDo(jetty,"GET","/chain/objects/uispec",null);
		assertEquals(200,response.getStatus());
		JSONObject generated=new JSONObject(response.getContent());
		JSONObject comparison=new JSONObject(getResourceString("collection-object.uispec"));
		///log.info(response.getContent());
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));
		
		// Intake
		response=jettyDo(jetty,"GET","/chain/intake/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("intake.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));
		// Acquisition
		response=jettyDo(jetty,"GET","/chain/acquisition/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("acquisition.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));
		// Person
		response=jettyDo(jetty,"GET","/chain/person/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("person.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));		
		// Organization
		response=jettyDo(jetty,"GET","/chain/organization/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("organization-authority.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
		// Object tab
		response=jettyDo(jetty,"GET","/chain/object-tab/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("object-tab.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
		// UserDetails tab
		response=jettyDo(jetty,"GET","/chain/users/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("users.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
		// Loanin tab
		response=jettyDo(jetty,"GET","/chain/loanin/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("loanin.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
		// Loanout tab
		response=jettyDo(jetty,"GET","/chain/loanout/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("loanout.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
		
		// Roles tab
		response=jettyDo(jetty,"GET","/chain/role/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("roles.uispec"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	

		// Find-Edit
		response=jettyDo(jetty,"GET","/chain/find-edit/uispec",null);
		assertEquals(200,response.getStatus());
		generated=new JSONObject(response.getContent());
		comparison=new JSONObject(getResourceString("find-edit.uispec"));
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(generated,comparison));	
	}
}
