package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVocabThroughWebapp {
	private static final Logger log=LoggerFactory.getLogger(TestVocabThroughWebapp.class);
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
		/*test if need to reset data - only reset it org auth are null
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person/",null);
		if(out.getStatus()<299){
			JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
			if(results.length()==0){
				jettyDo(jetty,"GET","/chain/reset",null);
			}
		}	
		*/		
	}

	@Test public void testInitialise() throws Exception{
		String vocabtype="languages";
		//String vocabtype="loanoutstatus";
		ServletTester jetty=setupJetty();
		// Create
		HttpTester out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"/initialize",null);
		// update and remove fields not in list
		//HttpTester out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"/refresh",null);
		assertTrue(out.getStatus()<300);
		
		
	}

	
	@Test public void testCRUDitem() throws Exception{
		String displayname = "XXXStuff";
		String displaynameUpdate = "XXXLessStuff";
		String vocabtype="languages";
		String testfield = "displayName";
		

		ServletTester jetty=setupJetty();
		// Create
		JSONObject data=new JSONObject("{'fields':{'"+testfield+"':'"+displayname+"'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/"+vocabtype+"/",data.toString());
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		// Read
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals(displayname,data.getString(testfield));
		// Update
		data=new JSONObject("{'fields':{'"+testfield+"':'"+displaynameUpdate+"'}}");
		out=jettyDo(jetty,"PUT","/chain/vocabularies"+url,data.toString());		
		assertTrue(out.getStatus()<300);
		// Read
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals(displaynameUpdate,data.getString(testfield));
		// Delete
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());		
		
	}
	
	@Test public void testList() throws Exception{
		String displayname = "XXXStuff";
		String vocabtype="languages";
		String testfield = "displayName";
		ServletTester jetty=setupJetty();
		
		// Create
		JSONObject data=new JSONObject("{'fields':{'"+testfield+"':'"+displayname+"'}}");
	    HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/"+vocabtype+"/",data.toString());              
	    assertTrue(out.getStatus()<300);
	    String url=out.getHeader("Location");
	    // Get List
		int resultsize =1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found=false;
		while(resultsize >0){
			out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"?pageSize=200&pageNum="+pagenum,null);
			pagenum++;
			assertTrue(out.getStatus()<299);
			JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");

			if(results.length()==0 || checkpagination.equals(results.getJSONObject(0).getString("csid"))){
				resultsize=0;
				break;
				//testing whether we have actually returned the same page or the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");

			for(int i=0;i<results.length();i++) {
				JSONObject entry=results.getJSONObject(i);
				if(entry.getString(testfield).toLowerCase().contains(displayname.toLowerCase())){
					found=true;
					resultsize=0;
				}
			}
		}
		assertTrue(found);
		// Delete
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());
		
		
	}
	
	@Test public void testSearch() throws Exception{
		String displayname = "XXXStuff";
		String vocabtype="languages";
		String testfield = "displayName";

		ServletTester jetty=setupJetty();
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'"+testfield+"':'"+displayname+"'}}");
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/"+vocabtype+"/",data.toString());		
		assertTrue(out.getStatus()<300);
		String url=out.getHeader("Location");
		
		out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"/search?query="+displayname,null);
		assertTrue(out.getStatus()<299);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString(testfield).toLowerCase().contains(displayname.toLowerCase()));
			assertEquals(entry.getString("number"),entry.getString(testfield));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		out=jettyDo(jetty,"DELETE","/chain/vocabularies"+url,null);
		assertTrue(out.getStatus()<299);
		out=jettyDo(jetty,"GET","/chain/vocabularies"+url,null);
		assertEquals(400,out.getStatus());
		
	}

	// Tests that a redirect goes to the expected place
	@Test public void testAutocompleteRedirect() throws Exception {
		ServletTester jetty=setupJetty();
		
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/source-vocab/inscriptionContentLanguage",null);
		log.info(out.getContent());
		assertTrue(out.getStatus()<299);
		JSONObject data=new JSONObject(out.getContent());
		String url=data.getString("url");
		assertEquals("/vocabularies/languages",url);
		
	}	
	//inscriptionContentLanguage
	
	
	
}
