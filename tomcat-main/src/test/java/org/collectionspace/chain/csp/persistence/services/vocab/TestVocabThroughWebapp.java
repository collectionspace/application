package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestVocabThroughWebapp {
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
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.go();
		String base=config_controller.getOption("services-url");		
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("storage","service");
		tester.setAttribute("store-url",base+"/cspace-services/");		
		tester.start();
		return tester;
	}
	
	@Test public void testAutocomplete() throws Exception {
		ServletTester jetty=setupJetty();
		jettyDo(jetty,"GET","/chain/quick-reset",null);
		HttpTester out=jettyDo(jetty,"GET","/chain/intake/autocomplete/depositor?q=Achmed+Abdullah&limit=150",null);
		assertTrue(out.getStatus()<299);
		String[] data=out.getContent().split("\n");
		for(int i=0;i<data.length;i++) {
			JSONObject entry=new JSONObject(data[i]);
			assertTrue(entry.getString("label").toLowerCase().contains("achmed abdullah"));
			assertTrue(entry.has("urn"));
		}
	}
	
	@Test public void testAuthoritiesSearch() throws Exception {
		ServletTester jetty=setupJetty();
		jettyDo(jetty,"GET","/chain/quick-reset",null);
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person/search?query=Achmed+Abdullah",null);
		assertTrue(out.getStatus()<299);
		System.err.println(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("name").toLowerCase().contains("achmed abdullah"));
			assertTrue(entry.has("refid"));
		}
	}

	@Test public void testAuthoritiesList() throws Exception {
		ServletTester jetty=setupJetty();
		jettyDo(jetty,"GET","/chain/quick-reset",null);
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person",null);
		assertTrue(out.getStatus()<299);
		System.err.println(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
		boolean found=false;
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			if(entry.getString("name").toLowerCase().contains("achmed abdullah"))
				found=true;
		}
		assertTrue(found);
	}
}
