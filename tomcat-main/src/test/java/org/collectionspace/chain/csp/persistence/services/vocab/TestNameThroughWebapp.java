/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameThroughWebapp{
	private static final Logger log=LoggerFactory.getLogger(TestNameThroughWebapp.class);
	private static TestBase tester = new TestBase();
//need a begin function that creates the default person if it is missing?
	@Before public void testCreateAuth() throws Exception {
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.GETData("/vocabularies/person/",jetty);
		log.info(out.getContent());
		JSONObject test = new JSONObject(out.getContent());
		if(test.has("isError") && test.getBoolean("isError")){
			//create the person authority
			JSONObject data=new JSONObject("{'fields':{'displayName':'Default Person Authority','shortIdentifier':'person','vocabType':'PersonAuthority'}}");
			out = tester.POSTData("/authorities/person/",data,jetty);	
		}

		out = tester.GETData("/vocabularies/persontest1/",jetty);
		log.info(out.getContent());
		test = new JSONObject(out.getContent());
		if(test.has("isError") && test.getBoolean("isError")){
			//create the person authority
			JSONObject data=new JSONObject("{'fields':{'displayName':'Test Person Authority 1','shortIdentifier':'persontest1','vocabType':'PersonAuthority'}}");
			out = tester.POSTData("/authorities/person/",data,jetty);	
		}
		
		out = tester.GETData("/vocabularies/persontest2/",jetty);
		log.info(out.getContent());
		test = new JSONObject(out.getContent());
		if(test.has("isError") && test.getBoolean("isError")){
			//create the person authority
			JSONObject data=new JSONObject("{'fields':{'displayName':'Test Person Authority 2','shortIdentifier':'persontest2','vocabType':'PersonAuthority'}}");
			out = tester.POSTData("/authorities/person/",data,jetty);	
		}

		out = tester.GETData("/vocabularies/organization/",jetty);
		log.info(out.getContent());
		JSONObject test2 = new JSONObject(out.getContent());
		if(test2.has("isError") && test2.getBoolean("isError")){
			//create the person authority
			JSONObject data=new JSONObject("{'fields':{'displayName':'Default Organization Authority','shortIdentifier':'organization','vocabType':'OrgAuthority'}}");
			out = tester.POSTData("/authorities/organization/",data,jetty);	
		}
		out = tester.GETData("/vocabularies/organizationtest/",jetty);
		log.info(out.getContent());
		test2 = new JSONObject(out.getContent());
		if(test2.has("isError") && test2.getBoolean("isError")){
			//create the person authority
			JSONObject data=new JSONObject("{'fields':{'displayName':'Test Organization Authority','shortIdentifier':'organizationtest','vocabType':'OrgAuthority'}}");
			out = tester.POSTData("/authorities/organization/",data,jetty);	
		}
	}
	
	//XXX change so creates person and then tests person exists
	@Test public  void testAutocomplete() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: Autocomplete: test_start");
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTNursultan Nazarbayev'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
			
		// Now test
		out = tester.GETData("/intake/autocomplete/depositor?q=XXXTESTNursultan&limit=150",jetty);
		JSONArray testdata = new JSONArray(out.getContent());
		for(int i=0;i<testdata.length();i++) {
			JSONObject entry=testdata.getJSONObject(i);
			assertTrue(entry.getString("label").toLowerCase().contains("xxxtestnursultan nazarbayev"));
			assertTrue(entry.has("urn"));
		}
		
		// Delete the entry from the database
		tester.DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: Autocomplete: test_end");	
		tester.stopJetty(jetty);
	}

	@Test public void testAutocompleteRedirect() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: AutocompleteRedirect: test_start");

		HttpTester out = tester.GETData("/acquisition/source-vocab/acquisitionAuthorizer",jetty);
		
		JSONArray data=new JSONArray(out.getContent());
		boolean test = false;
		for(int i=0;i<data.length();i++){
			String url=data.getJSONObject(i).getString("url");
			if(url.equals("/vocabularies/person")){
				test=true;
			}
		}
		assertTrue("correct vocab not found",test);
		

		out = tester.GETData("/cataloging/source-vocab/contentOrganization",jetty);

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
		tester.stopJetty(jetty);
		
	}
	
	//this isn't the right test... what is the right test?
	@Test public void testAutocompleteVocabRedirect() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: AutocompleteVocabRedirect: test_start");
		HttpTester out = tester.GETData("/acquisition/source-vocab/acquisitionAuthorizer",jetty);
		

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
		tester.stopJetty(jetty);
	}
	
	@Test public void testAuthoritiesSearch() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: AuthoritiesSearch: test_start");
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTJacob Zuma'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	

		log.info(out.getContent());
		String url=out.getHeader("Location");

		out = tester.GETData("/authorities/person/search?query=XXXTESTJacob+Zuma",jetty);

		log.info(out.getContent());
		
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestjacob zuma"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		tester.DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: AuthoritiesSearch: test_end");
		tester.stopJetty(jetty);
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testAuthoritiesList() throws Exception {
		HttpTester out=tester.GETData("/authorities/person",jetty);
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

	//DISABLED UNTIL I work out what it wrong with the query @Test 
	public void testNamesAdvSearch() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: NamesSearch: test_start");
		//tester.GETData("/quick-reset",jetty);
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTRaul Castro'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		JSONObject payload = new JSONObject();
		JSONObject searchfields = new JSONObject();
		
		searchfields.put("displayName", "XXXTestR*");
		
		payload.put("operation", "or");
		payload.put("fields", searchfields);
		
		out=tester.POSTData("/vocabularies/person/search",payload,jetty,"GET");

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		tester.DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: NamesSearch: test_start");
	}
	@Test public void testNamesSearch() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: NamesSearch: test_start");
		//tester.GETData("/quick-reset",jetty);
		// Create the entry we are going to check for
		JSONObject data=new JSONObject("{'fields':{'displayName':'XXXTESTRaul Castro'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		
		out=tester.GETData("/vocabularies/person/search?query=XXXTESTRaul+Castro",jetty);

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString("displayName").toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		
		// Delete the entry from the database
		tester.DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: NamesSearch: test_start");
		tester.stopJetty(jetty);
	}

	// XXX failing due to pagination - reinsert when pagination works
	/*
	@Test public void testNamesList() throws Exception {
		HttpTester 
		out = tester.GETData("/vocabularies/person",jetty);

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
	@Test public void testPersonWithContactAuthorityCRUD() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: PersonWithContactAuthorityCRUD: test_start");
		JSONObject data=new JSONObject("{'csid': '', 'fields': {'salutation': 'Your Majaesty','termStatus': 'under review','title': 'Miss', 'gender': 'female','displayName': 'bob','nameNote': 'sdf','bioNote': 'sdfsdf', 'foreName': 'sdf', 'middleName': 'sdf', 'surName': 'sdf', 'nameAdditions': 'sdf', 'initials': 'sdf', 'contact': [{'addressType': 'AAA', 'addressPlace': 'AAA', 'web': 'AAA', 'email': 'AAA','telephoneNumber': 'AAA', 'faxNumber': 'AAA'}, {'addressType': 'BBB','addressPlace': 'BVV','web': 'VVV', 'email': 'VVV','telephoneNumber': 'VV','faxNumber': 'VV' }]}}}");

		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		log.info(out.getContent());
		log.info("NAME: PersonWithContactAuthorityCRUD: test_end");
		tester.stopJetty(jetty);
	}
	
	//XXX disable until soft delete works better everywhere @Test 
	public void testNames2vocabsCreateSearchDelete() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: NamesCreateUpdateDelete: test_start");
		JSONObject data=new JSONObject();
		HttpTester out ;
		// Create
		
		log.info("NAME: NamesCreateUpdateDelete person test: CREATE");
		data=new JSONObject("{'fields':{'displayName':'TESTTESTFred Bloggers', 'contact': {'addressType': 'AAA', 'addressPlace': 'AAA', 'web': 'AAA', 'email': 'AAA','telephoneNumber': 'AAA', 'faxNumber': 'AAA'}}}");
		out = tester.POSTData("/vocabularies/persontest1/",data,jetty);
		String url=out.getHeader("Location");
		log.info(out.getContent());
		
		log.info("NAME: NamesCreateUpdateDelete person default: CREATE");
		data=new JSONObject("{'fields':{'displayName':'DDDDTESTFred Bloggers', 'contact': {'addressType': 'AAA', 'addressPlace': 'AAA', 'web': 'AAA', 'email': 'AAA','telephoneNumber': 'AAA', 'faxNumber': 'AAA'}}}");
		out = tester.POSTData("/vocabularies/persontest2/",data,jetty);
		String url2=out.getHeader("Location");
		log.info(out.getContent());

		//all person authorities
		
		out = tester.GETData("/authorities/person/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results.length());

		out = tester.GETData("/authorities/person/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results12=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results12.length());

		out = tester.GETData("/authorities/person/search?query=Bloggers",jetty);
		log.info(out.getContent());
		JSONArray results2=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(2,results2.length());
		
		//specific person authority
		out=tester.GETData("/vocabularies/persontest1/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results3=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results3.length());
		out=tester.GETData("/vocabularies/persontest1/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results32=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(0,results32.length());
		out=tester.GETData("/vocabularies/persontest1/search?query=Bloggers",jetty);
		log.info(out.getContent());
		JSONArray results31=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results31.length());
		
		//specific person authority
		out=tester.GETData("/vocabularies/persontest2/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results4=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(0,results4.length());
		out=tester.GETData("/vocabularies/persontest2/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results42=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results42.length());
		out=tester.GETData("/vocabularies/persontest2/search?query=Bloggers",jetty);
		log.info(out.getContent());
		JSONArray results41=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results41.length());
		
		//all person authorities
		out = tester.GETData("/person/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results1=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results1.length());

		out = tester.GETData("/person/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results13=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results13.length());

		out = tester.GETData("/person/search?query=Bloggers",jetty);
		log.info(out.getContent());
		JSONArray results11=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(2,results11.length());

		// Read
		// Delete
	log.info("NAME: NamesCreateUpdateDelete: DELETE");
		tester.DELETEData("/vocabularies/"+url,jetty);
		tester.DELETEData("/vocabularies/"+url2,jetty);
		log.info("NAME: NamesCreateUpdateDelete: test_end");
		}
		
	@Test 
	public void testNamesCreateUpdateDelete() throws Exception {
		ServletTester jetty = tester.setupJetty();
			log.info("NAME: NamesCreateUpdateDelete: test_start");
			// Create
			log.info("NAME: NamesCreateUpdateDelete: CREATE");
			JSONObject data=new JSONObject("{'csid': '', 'fields': {'displayName': 'XXXTESTFred Bloggs','contact': {'emailGroup': [{'email': 'test@example.com','emailType': 'home' }],'addressGroup': [{'addressPlace1': 'addressPlace1','addressPlace2': 'addressPlace2','addressMunicipality': 'addressMunicipality','addressStateOrProvince': 'addressStateOrProvince', 'addressPostCode': 'addressPostCode','addressCountry': 'addressCountry' }, {'addressPlace1': 'SECOND_addressPlace1','addressPlace2': 'SECOND_addressPlace2','addressMunicipality': 'SECOND_addressMunicipality','addressStateOrProvince': 'SECOND_addressStateOrProvince','addressPostCode': 'SECOND_addressPostCode', 'addressCountry': 'SECOND_addressCountry'}]} }}");
			HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);
			String url=out.getHeader("Location");
			log.info(out.getContent());
			JSONObject datad = new JSONObject(out.getContent());
			JSONObject updatefields = datad;
			if(datad.has("fields")){
				updatefields = new JSONObject(out.getContent()).getJSONObject("fields");
			}
			// Read
		log.info("NAME: NamesCreateUpdateDelete: READ");
		out = tester.GETData("/vocabularies"+url,jetty);
		log.info(out.getContent());
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTFred Bloggs",data.getString("displayName"));
		// Update
		log.info("NAME: NamesCreateUpdateDelete: UPDATE"+updatefields.toString());
		updatefields.put("displayName", "XXXTESTOwain Glyndwr");
		updatefields.getJSONObject("contact").getJSONArray("emailGroup").getJSONObject(0).put("emailType", "newtype");
		data = new JSONObject();
		data.put("fields", updatefields);
		//data=new JSONObject("{'fields':{'displayName':'XXXTESTOwain Glyndwr', 'contact': {'addressType': 'DDD',  'faxNumber': 'ADDDDAA'}}}");
		out=tester.PUTData("/vocabularies"+url,data,jetty);	
		log.info(out.getContent());	
		// Read
		log.info("NAME: NamesCreateUpdateDelete: READ");
		out = tester.GETData("/vocabularies"+url,jetty);

		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		assertEquals("XXXTESTOwain Glyndwr",data.getString("displayName"));
		assertEquals("newtype",data.getJSONObject("contact").getJSONArray("emailGroup").getJSONObject(0).getString("emailType"));
		// Delete
		log.info("NAME: NamesCreateUpdateDelete: DELETE");
		tester.DELETEData("/vocabularies/"+url,jetty);
		log.info("NAME: NamesCreateUpdateDelete: test_end");
		tester.stopJetty(jetty);
	}
	
	private String getName(String name) throws Exception
	{
		ServletTester jetty = tester.setupJetty();
		// Create
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);	
		String url=out.getHeader("Location");
		// Read
		out = tester.GETData("/vocabularies"+url,jetty);
		
		data=new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"),url.split("/")[2]);
		return data.getString("displayName");
	}
	
	private void setName(String name,ServletTester jetty) throws Exception
	{
		// Update
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);		
		String url=out.getHeader("Location");
		out=tester.PUTData("/vocabularies"+url,data,jetty);	
	}
	
	private void deleteName(String name,ServletTester jetty) throws Exception
	{
		// Delete
		JSONObject data=new JSONObject("{'fields':{'displayName':'" + name + "'}}");
		HttpTester out = tester.POSTData("/vocabularies/person/",data,jetty);		
		String url=out.getHeader("Location");
		tester.DELETEData("/vocabularies/"+url,jetty);
	}
	

	
}
