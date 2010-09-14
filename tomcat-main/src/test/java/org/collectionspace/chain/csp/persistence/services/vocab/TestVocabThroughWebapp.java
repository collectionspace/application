package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVocabThroughWebapp extends TestBase {
	private static final Logger log=LoggerFactory.getLogger(TestVocabThroughWebapp.class);
	

	
	@BeforeClass public void reset() throws Exception {
		ServletTester jetty=setupJetty();
		/*test if need to reset data - only reset it org auth are null
		HttpTester out=jettyDo(jetty,"GET","/chain/authorities/person/?pageSize=2",null);
		if(out.getStatus()<299){
			JSONArray results=new JSONObject(out.getContent()).getJSONArray("items");
			if(results.length()==0){
				jettyDo(jetty,"GET","/chain/reset/nodelete",null);
			}
		}	
		*/
	}

	@Test public void testInitialise() throws Exception{
		String vocabtype="languages";
		HttpTester out;
		//String vocabtype="loanoutstatus";
		ServletTester jetty=setupJetty();
		// Create a single vocab
//		out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"/initialize",null);
//		assertTrue(out.getStatus()<300);
		
		//create all vocabularies in <record id="vocab"
		out=jettyDo(jetty,"GET","/chain/authorities/vocab/initialize",null);
		log.info(out.getContent());
		assertTrue(out.getStatus()<300);

		// update and remove fields not in list
//		out=jettyDo(jetty,"GET","/chain/vocabularies/"+vocabtype+"/refresh",null);
//		assertTrue(out.getStatus()<300);
		
		// update and remove fields not in each list within an authority
//		out=jettyDo(jetty,"GET","/chain/authorities/vocab/refresh",null);
//		assertTrue(out.getStatus()<300);
		
		
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
		assertTrue(out.getStatus()<299);

		JSONArray data=new JSONArray(out.getContent());
		boolean test = false;
		for(int i=0;i<data.length();i++){
			String url=data.getJSONObject(i).getString("url");
			if(url.equals("/vocabularies/languages")){
				test=true;
			}
		}
		assertTrue("correct vocab not found",test);
		
		
	}	
	//inscriptionContentLanguage
	
	
	
}
