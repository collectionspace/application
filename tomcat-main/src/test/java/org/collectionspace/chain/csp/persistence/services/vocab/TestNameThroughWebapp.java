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

public class TestNameThroughWebapp extends TestBase{
	private static final Logger log=LoggerFactory.getLogger(TestNameThroughWebapp.class);
	

	
	//XXX change so creates person and then tests person exists
	@Test public  void testAutocomplete() throws Exception {
		log.info("NAME: Autocomplete: test_start");
			ServletTester jetty=setupJetty();
			// Create the entry we are going to check for
			JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTNursultan Nazarbayev'}}");
			HttpTester out = POSTData("/vocabularies/person/",data,jetty);	
			String url=out.getHeader("Location");
			
		// Now test
		out = GETData("/intake/autocomplete/depositor?q=XXXTESTNursultan&limit=150",jetty);
		assertTrue(out.getStatus()<299);
		JSONArray testdata = new JSONArray(out.getContent());
		for(int i=0;i<testdata.length();i++) {
			JSONObject entry=testdata.getJSONObject(i);
			assertTrue(entry.getString("label").toLowerCase().contains("xxxtestnursultan nazarbayev"));
			assertTrue(entry.has("urn"));
		}
		
		// Delete the entry from the database
		DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: Autocomplete: test_end");	
	}

	@Test public void testAutocompleteRedirect() throws Exception {
		log.info("NAME: AutocompleteRedirect: test_start");
		ServletTester jetty=setupJetty();

		HttpTester out = GETData("/acquisition/source-vocab/acquisitionAuthorizer",jetty);
		
		JSONArray data=new JSONArray(out.getContent());
		boolean test = false;
		for(int i=0;i<data.length();i++){
			String url=data.getJSONObject(i).getString("url");
			if(url.equals("/vocabularies/person")){
				test=true;
			}
		}
		assertTrue("correct vocab not found",test);
		

		out = GETData("/objects/source-vocab/contentOrganization",jetty);

		data=new JSONArray(out.getContent());
		test = false;
		for(int i=0;i<data.length();i++){
			String url=data.getJSONObject(i).getString("url");
			if(url.equals("/vocabularies/organization")){
				test=true;
			}
		}
		assertTrue("correct vocab not found",test);
		log.info("NAME: AutocompleteRedirect: test_end");
		
	}
	
	//this isn't the right test... what is the right test?
	@Test public void testAutocompleteVocabRedirect() throws Exception {
		log.info("NAME: AutocompleteVocabRedirect: test_start");
		ServletTester jetty=setupJetty();
		HttpTester out = GETData("/acquisition/source-vocab/acquisitionAuthorizer",jetty);
		

		JSONArray data=new JSONArray(out.getContent());
		boolean test = false;
		for(int i=0;i<data.length();i++){
			String url=data.getJSONObject(i).getString("url");
			if(url.equals("/vocabularies/person")){
				test=true;
			}
		}
		assertTrue("correct vocab not found",test);
		
		log.info("NAME: AutocompleteVocabRedirect: test_end");
	}
	
	@Test public void testAuthoritiesSearch() throws Exception {
		log.info("NAME: AuthoritiesSearch: test_start");
		ServletTester jetty=setupJetty();
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTJacob Zuma'}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);	

		String url=out.getHeader("Location");

		out = GETData("/authorities/person/search?query=XXXTESTJacob+Zuma",jetty);

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestjacob zuma"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: AuthoritiesSearch: test_end");
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testAuthoritiesList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=GETData("/authorities/person",jetty);
		assertTrue(out.getStatus()<299);
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
		log.info("NAME: NamesSearch: test_start");
		ServletTester jetty=setupJetty();
		//GETData("/quick-reset",jetty);
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTRaul Castro'}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		
		out=GETData("/vocabularies/person/search?query=XXXTESTRaul+Castro",jetty);

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: NamesSearch: test_start");
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testNamesList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester 
		out = GETData("/vocabularies/person",jetty);
		assertTrue(out.getStatus()<299);

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
		log.info("NAME: Get: test_start");
		// Create the name we want to test against, and after testing - delete it
		String testName = "XXXTESTHamid Karzai"; 

		setName(testName);
		// Carry out the test
		ServletTester jetty=setupJetty();
		HttpTester out = GETData("/vocabularies/person/search?query=" + testName.replace(' ','+'),jetty);
		// Find candidate
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
//		assertEquals(1,results.length());
		JSONObject entry=results.getJSONObject(0);
		String csid=entry.getString("csid");
		out = GETData("/vocabularies/person/"+csid,jetty);
		JSONObject fields=new JSONObject(out.getContent()).getJSONObject("fields");

		assertEquals(csid,fields.getString("csid"));
		assertEquals(testName,fields.getString("displayName"));
		// Now remove the name from the database
		deleteName(testName);
		log.info("NAME: Get: test_end");
	}
	
	@Test public void testPersonWithContactAuthorityCRUD() throws Exception {
		log.info("NAME: PersonWithContactAuthorityCRUD: test_start");
		ServletTester jetty=setupJetty();
		JSONObject data=new JSONObject("{'csid': '', 'fields': {'salutation': 'Your Majaesty','termStatus': 'under review','title': 'Miss', 'gender': 'female','displayName': 'bob','nameNote': 'sdf','bioNote': 'sdfsdf', 'foreName': 'sdf', 'middleName': 'sdf', 'surName': 'sdf', 'nameAdditions': 'sdf', 'initials': 'sdf', 'contact': [{'addressType': 'AAA', 'addressPlace': 'AAA', 'web': 'AAA', 'email': 'AAA','telephoneNumber': 'AAA', 'faxNumber': 'AAA'}, {'addressType': 'BBB','addressPlace': 'BVV','web': 'VVV', 'email': 'VVV','telephoneNumber': 'VV','faxNumber': 'VV' }]}}}");

		HttpTester out = POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		log.info(out.getContent());
		log.info("NAME: PersonWithContactAuthorityCRUD: test_end");
	}
	
	@Test public void testNamesCreateUpdateDelete() throws Exception {
		log.info("NAME: NamesCreateUpdateDelete: test_start");
		ServletTester jetty=setupJetty();
		// Create
		log.info("NAME: NamesCreateUpdateDelete: CREATE");
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTFred Bloggs', 'contact': {'addressType': 'AAA', 'addressPlace': 'AAA', 'web': 'AAA', 'email': 'AAA','telephoneNumber': 'AAA', 'faxNumber': 'AAA'}}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);
		String url=out.getHeader("Location");
		log.info(out.getContent());
		JSONObject updatefields = new JSONObject(out.getContent()).getJSONObject("fields");
		// Read
		log.info("NAME: NamesCreateUpdateDelete: READ");
		out = GETData("/vocabularies"+url,jetty);
		log.info(out.getContent());
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTFred Bloggs",data.getString("displayName"));
		// Update
		log.info("NAME: NamesCreateUpdateDelete: UPDATE");
		updatefields.put("displayName", "XXXTESTOwain Glyndwr");
		updatefields.getJSONObject("contact").put("addressPlace", "addressPlace");
		data = new JSONObject();
		data.put("fields", updatefields);
		//data=new JSONObject("{'fields':{'displayName':'XXXTESTOwain Glyndwr', 'contact': {'addressType': 'DDD',  'faxNumber': 'ADDDDAA'}}}");
		out=PUTData("/vocabularies"+url,data,jetty);	
		log.info(out.getContent());	
		// Read
		log.info("NAME: NamesCreateUpdateDelete: READ");
		out = GETData("/vocabularies"+url,jetty);

		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTOwain Glyndwr",data.getString("displayName"));
		// Delete
		log.info("NAME: NamesCreateUpdateDelete: DELETE");
		DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: NamesCreateUpdateDelete: test_end");	
	}
	
	private String getName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Create
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		// Read
		out = GETData("/vocabularies"+url,jetty);
		
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		return data.getString("displayName");
	}
	
	private void setName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Update
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);		
		String url=out.getHeader("Location");
		out=PUTData("/vocabularies"+url,data,jetty);	
	}
	
	private void deleteName(String name) throws Exception
	{
		ServletTester jetty=setupJetty();
		// Delete
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = POSTData("/vocabularies/person/",data,jetty);		
		String url=out.getHeader("Location");
		DELETEData("/vocabularies/"+url,jetty);
	}
	

	
}
