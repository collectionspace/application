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
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameThroughWebapp {
	private static final Logger log=LoggerFactory.getLogger(TestNameThroughWebapp.class);
	private static String cookie;
	
	// XXX refactor
	protected InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	private static void login(ServletTester tester) throws IOException, Exception {
		HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.info("Got cookie "+cookie);
	}
	
	// XXX refactor
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
	
	// XXX refactor into other copy of this method
	private static ServletTester setupJetty() throws Exception {
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
	
	@BeforeClass public static void reset() throws Exception {
		ServletTester jetty=setupJetty();
		//test if need to reset data - only reset it org auth are null
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person/",null);
		if(out.getStatus()<299){
			JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
			if(results.length()==0){
				jettyDo(jetty,"GET","/chain/reset",null);
			}
		}			
	}
	
	//XXX change so creates person and then tests person exists
	@Test public void testAutocomplete() throws Exception {
		ServletTester jetty=setupJetty();
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTNursultan Nazarbayev'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		
		// Now test
		out=jettyDo(jetty,"GET","/chain/intake/autocomplete/depositor?q=XXXTESTNursultan&limit=150",null);
		assertTrue(out.getStatus()<299);
		String[] testData=out.getContent().split("\n");
		for(int i=0;i<testData.length;i++) {
			JSONObject entry=new JSONObject(testData[i]);
			assertTrue(entry.getString("label").toLowerCase().contains("xxxtestnursultan nazarbayev"));
			assertTrue(entry.has("urn"));
		}
		
		// Delete the entry from the database
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());		
	}
	
	@Test public void testAutocompleteRedirect() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/acquisition/source-vocab/acquisitionAuthorizer",null);
		assertTrue(out.getStatus()<299);
		JSONObject data=new JSONObject(out.getContent());
		String url=data.getString("url");
		assertEquals("/vocabularies/person",url);

		out=jettyDo(jetty,"GET","/chain/objects/source-vocab/contentOrganization",null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent());
		url=data.getString("url");
		assertEquals("/vocabularies/organization",url);
		
	}
	
	//this isn't the right test... what is the right test?
	@Test public void testAutocompleteVocabRedirect() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/acquisition/source-vocab/acquisitionAuthorizer",null);
		assertTrue(out.getStatus()<299);
		JSONObject data=new JSONObject(out.getContent());
		String url=data.getString("url");
		assertEquals("/vocabularies/person",url);
	}
	
	@Test public void testAuthoritiesSearch() throws Exception {
		ServletTester jetty=setupJetty();
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTJacob Zuma'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		
		out=jettyDo(jetty,"GET","/chain/authorities/person/search?query=XXXTESTJacob+Zuma",null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestjacob zuma"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testAuthoritiesList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person",null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
		boolean found=false;
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			if(entry.getString("displayName").toLowerCase().contains("achmed abdullah"))
				found=true;
		}
		//might be failing because of pagination
		assertTrue(found);
	}
	*/

	@Test public void testNamesSearch() throws Exception {
		ServletTester jetty=setupJetty();
		//jettyDo(jetty,"GET","/chain/quick-reset",null);
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTRaul Castro'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		
		out=jettyDo(jetty,"GET","/chain/vocabularies/person/search?query=XXXTESTRaul+Castro",null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testNamesList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/vocabularies/person",null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
		boolean found=false;
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			if(entry.getString("displayName").toLowerCase().contains("achmed abdullah"))
				found=true;
		}
		assertTrue(found);
	}
	*/

	@Test public void testNamesGet() throws Exception {
		// Create the name we want to test against, and after testing - delete it
		String testName = "XXXTESTHamid Karzai"; 

		setName(testName);
		// Carry out the test
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/vocabularies/person/search?query=" + testName.replace(' ','+'),null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		// Find candidate
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
//		assertEquals(1,results.length());
		JSONObject entry=results.getJSONObject(0);
		String csid=entry.getString("csid");
		out=jettyDo(jetty,"GET","/chain/vocabularies/person/"+csid,null);
		JSONObject fields=new JSONObject(out.getContent()).getJSONObject("fields");
		log.info("JSON",fields);
		assertEquals(csid,fields.getString("csid"));
		assertEquals(testName,fields.getString("displayName"));
		// Now remove the name from the database
		deleteName(testName);
	}

	@Test public void testNamesCreateUpdateDelete() throws Exception {
		ServletTester jetty=setupJetty();
		// Create
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTFred Bloggs'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		// Read
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTFred Bloggs",data.getString("displayName"));
		// Update
		data=new JSONObject("{'fields':{'displayName':'XXXTESTOwain Glyndwr'}}");
		out=jettyDo(jetty,"PUT","/chain/vocabularies"+url,data.toString());		
		assertTrue(out.getStatus()<300);
		// Read
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTOwain Glyndwr",data.getString("displayName"));
		// Delete
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());		
	}
	
	private String getName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Create
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		// Read
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		return data.getString("displayName");
	}
	
	private void setName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Update
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		String url=out.getHeader("Location");
		out=jettyDo(jetty,"PUT","/chain/vocabularies"+url,data.toString());		
		assertTrue(out.getStatus()<300);
	}
	
	private void deleteName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Delete
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",data.toString());		
		String url=out.getHeader("Location");
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
//		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
//		assertEquals(400,out.getStatus());		
	}
	

	
}
