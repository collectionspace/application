package org.collectionspace.chain.storage;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.util.json.JSONTestUtil;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestServiceThroughWebapp {
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	private String getResourceString(String name) throws IOException {
		InputStream in=getResource(name);
		return IOUtils.toString(in);
	}
	
	// XXX refactor
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
	
	// XXX refactor into other copy of this method
	private ServletTester setupJetty() throws Exception {
		BootstrapConfigController config_controller=new BootstrapConfigController(null);
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		String base=config_controller.getOption("services-url");		
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("storage","service");
		tester.setAttribute("store-url",base+"/helloworld/cspace-nuxeo/");		
		tester.start();
		return tester;
	}
	
	@Test public void testCollectionObjectBasic() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/test-json-handle.tmp",getResourceString("obj3.json"));	
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertTrue(JSONTestUtil.checkJSONEquiv(new JSONObject(getResourceString("obj3.json")),new JSONObject(out.getContent())));
		out=jettyDo(jetty,"PUT","/chain/objects/test-json-handle.tmp",getResourceString("obj4.json"));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertTrue(JSONTestUtil.checkJSONEquiv(new JSONObject(getResourceString("obj4.json")),new JSONObject(out.getContent())));		
		out=jettyDo(jetty,"DELETE","/chain/objects/test-json-handle.tmp",null);
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertTrue(out.getStatus()!=200); // XXX should be 404
	}
}
