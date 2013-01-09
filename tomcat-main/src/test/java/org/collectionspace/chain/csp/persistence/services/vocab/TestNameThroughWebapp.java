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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameThroughWebapp{
	private static final Logger log=LoggerFactory.getLogger(TestNameThroughWebapp.class);
	private static TestBase tester = new TestBase();
	
	
//need a begin function that creates the default person if it is missing?
	@BeforeClass public static void testCreateAuth() throws Exception {
		ServletTester jetty = tester.setupJetty();
		
		HttpTester out = tester.GETData(TestBase.AUTHS_INIT_PATH, jetty);
		log.info(out.getContent());

		JSONObject test = null;
	}
	
	private static final String PERSON_TERMLIST_ELEMENT = "personTermGroup";
	private static final String ORG_TERMLIST_ELEMENT = "orgTermGroup";
	private static final String TERM_DISPLAYNAME_ELEMENT = "termDisplayName";
	private static final String DISPLAY_NAMES = "displayNames";
	private static final String DISPLAY_NAME = "displayName";

	private static JSONObject createTrivialAuthItem(String termGroup, String name) throws JSONException {
		JSONObject item=new JSONObject();
		JSONArray termInfoArray = new JSONArray();
		JSONObject termInfo = new JSONObject();
		termInfo.put(TERM_DISPLAYNAME_ELEMENT, name);
		termInfoArray.put(termInfo);
		JSONObject fields = new JSONObject();
		fields.put(termGroup, termInfoArray);
		item.put("fields", fields);
		return item;
	}
	
	//XXX change so creates person and then tests person exists
	@Test public  void testAutocomplete() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: Autocomplete: test_start");
		// Create the entry we are going to check for
		JSONObject data=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, "XXXTESTNursultan Nazarbayev");
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	
		String url=out.getHeader("Location");
			
		// Now test
		out = tester.GETData("/intake/autocomplete/depositor?q=XXXTESTNursultan&limit=150",jetty);
		JSONArray testdata = new JSONArray(out.getContent());
		for(int i=0;i<testdata.length();i++) {
			JSONObject entry=testdata.getJSONObject(i);
			JSONArray displayNames = entry.getJSONArray(DISPLAY_NAMES);
			assertTrue(displayNames.getString(0).toLowerCase().contains("xxxtestnursultan nazarbayev"));
			assertTrue(entry.has("baseUrn"));
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
			if(url.equals(TestBase.MAIN_PERSON_INSTANCE_PATH)){
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
			if(url.equals(TestBase.MAIN_PERSON_INSTANCE_PATH)){
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
		JSONObject data=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, "XXXTESTJacob Zuma");
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	

		log.info(out.getContent());
		String url=out.getHeader("Location");

		out = tester.GETData("/authorities/person/search?query=XXXTESTJacob+Zuma",jetty);

		log.info(out.getContent());
		
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("xxxtestjacob zuma"));
			assertEquals(entry.getString("number"),entry.getString(TERM_DISPLAYNAME_ELEMENT));
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
		JSONObject data=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, "XXXTESTRaul Castro");
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	
		String url=out.getHeader("Location");
		JSONObject payload = new JSONObject();
		JSONObject searchfields = new JSONObject();
		
		searchfields.put(TERM_DISPLAYNAME_ELEMENT, "XXXTESTR*");
		
		payload.put("operation", "or");
		payload.put("fields", searchfields);
		
		out=tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/search",payload,jetty,"GET");

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			JSONArray displayNames = entry.getJSONArray(DISPLAY_NAMES);
			assertTrue(displayNames.getString(0).toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString(TERM_DISPLAYNAME_ELEMENT));
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
		JSONObject data=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, "XXXTESTRaul Castro");
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	
		String url=out.getHeader("Location");
		
		out=tester.GETData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/search?query=XXXTESTRaul+Castro",jetty);

		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		for(int i=0;i<results.length();i++) {
			JSONObject entry=results.getJSONObject(i);
			assertTrue(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("xxxtestraul castro"));
			assertEquals(entry.getString("number"),entry.getString(TERM_DISPLAYNAME_ELEMENT));
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
	
	private JSONObject simplePerson(String name) throws JSONException {
		StringBuilder sb = new StringBuilder();
		sb.append("{'fields': {'");
		sb.append(PERSON_TERMLIST_ELEMENT); 
		sb.append("':[{'"); 
		sb.append(TERM_DISPLAYNAME_ELEMENT);
		sb.append("': '");
		sb.append(name);
		sb.append("'}]}}");
		String full = sb.toString();
		JSONObject data=new JSONObject(full);
		return data;
	}

	private JSONObject personWithContact(String name, boolean fFull) throws JSONException {
		StringBuilder sb = new StringBuilder();
		sb.append("{'csid': '', 'fields':");
		sb.append(" {'"+PERSON_TERMLIST_ELEMENT+"':[{'"); 
		sb.append(TERM_DISPLAYNAME_ELEMENT+"': '"+name+"'");
		if(fFull) {
			sb.append(", 'salutation': 'Your Majaesty','termStatus': 'under review',");
			sb.append("'title': 'Miss', 'foreName': 'sdf', 'middleName': 'sdf', ");
			sb.append(" 'surName': 'sdf', 'nameAdditions': 'sdf', 'initials': 'sdf'}],");
			sb.append(" 'gender': 'female', 'nameNote': 'sdf','bioNote': 'sdfsdf',");
		} else {
			sb.append("}],");
		}
		//'contact': {'emailGroup': [{'email': 'test@example.com','emailType': 'home' }],
		//            'addressGroup': [{'addressPlace1': 'addressPlace1','addressPlace2': 'addressPlace2','addressMunicipality': 'addressMunicipality','addressStateOrProvince': 'addressStateOrProvince', 'addressPostCode': 'addressPostCode','addressCountry': 'addressCountry' },
		//				               {'addressPlace1': 'SECOND_addressPlace1','addressPlace2': 'SECOND_addressPlace2','addressMunicipality': 'SECOND_addressMunicipality','addressStateOrProvince': 'SECOND_addressStateOrProvince','addressPostCode': 'SECOND_addressPostCode', 'addressCountry': 'SECOND_addressCountry'}
		//                            ]}
		sb.append(" 'contact': {'emailGroup': [{'email': 'test@example.com','emailType': 'home' }],");
		sb.append("'addressGroup': [{'addressPlace1': 'addressPlace1','addressPlace2': 'addressPlace2','addressMunicipality': 'addressMunicipality','addressStateOrProvince': 'addressStateOrProvince', 'addressPostCode': 'addressPostCode','addressCountry': 'addressCountry' },");
		sb.append("{'addressPlace1': 'SECOND_addressPlace1','addressPlace2': 'SECOND_addressPlace2','addressMunicipality': 'SECOND_addressMunicipality','addressStateOrProvince': 'SECOND_addressStateOrProvince','addressPostCode': 'SECOND_addressPostCode', 'addressCountry': 'SECOND_addressCountry'}");
		sb.append("]}}}");
		JSONObject data=new JSONObject(sb.toString());
		return data;
	}
	
	@Test public void testPersonWithContactAuthorityCRUD() throws Exception {
		ServletTester jetty = tester.setupJetty();
		log.info("NAME: PersonWithContactAuthorityCRUD: test_start");
		
		JSONObject data = personWithContact("bob", true);

		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	
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
		data = personWithContact("TESTTESTFred Bloggers", false);
		out = tester.POSTData(TestBase.SECOND_PERSON_INSTANCE_PATH+"/",data,jetty);
		String url=out.getHeader("Location");
		log.info(out.getContent());
		
		log.info("NAME: NamesCreateUpdateDelete person default: CREATE");
		data = personWithContact("DDDDTESTFred Bloggers", false);
		out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);
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
		out=tester.GETData(TestBase.SECOND_PERSON_INSTANCE_PATH+"/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results3=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results3.length());
		out=tester.GETData(TestBase.SECOND_PERSON_INSTANCE_PATH+"/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results32=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(0,results32.length());
		out=tester.GETData(TestBase.SECOND_PERSON_INSTANCE_PATH+"/search?query=Bloggers",jetty);
		log.info(out.getContent());
		JSONArray results31=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results31.length());
		
		//specific person authority
		out=tester.GETData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/search?query=TESTTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results4=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(0,results4.length());
		out=tester.GETData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/search?query=DDDDTESTFred",jetty);
		log.info(out.getContent());
		JSONArray results42=new JSONObject(out.getContent()).getJSONArray("results");
		assertEquals(1,results42.length());
		out=tester.GETData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/search?query=Bloggers",jetty);
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
		//JSONObject data=new JSONObject("{'csid': '', 'fields': {'displayName': 'XXXTESTFred Bloggs','contact': {'emailGroup': [{'email': 'test@example.com','emailType': 'home' }],'addressGroup': [{'addressPlace1': 'addressPlace1','addressPlace2': 'addressPlace2','addressMunicipality': 'addressMunicipality','addressStateOrProvince': 'addressStateOrProvince', 'addressPostCode': 'addressPostCode','addressCountry': 'addressCountry' }, {'addressPlace1': 'SECOND_addressPlace1','addressPlace2': 'SECOND_addressPlace2','addressMunicipality': 'SECOND_addressMunicipality','addressStateOrProvince': 'SECOND_addressStateOrProvince','addressPostCode': 'SECOND_addressPostCode', 'addressCountry': 'SECOND_addressCountry'}]} }}");
		JSONObject data = personWithContact("XXXTESTFred Bloggs", true);
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);
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
		JSONArray termList = data.getJSONArray(PERSON_TERMLIST_ELEMENT);
		assertEquals("XXXTESTFred Bloggs",
				termList.getJSONObject(0).getString(TERM_DISPLAYNAME_ELEMENT));
		// Update
		log.info("NAME: NamesCreateUpdateDelete: UPDATE"+updatefields.toString());
		termList = updatefields.getJSONArray(PERSON_TERMLIST_ELEMENT);
		termList.getJSONObject(0).put(TERM_DISPLAYNAME_ELEMENT,"XXXTESTOwain Glyndwr");
		assertTrue("testNamesCreateUpdateDelete: Fetched person has no contact info!", 
				updatefields.has("contact"));
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
		termList = data.getJSONArray(PERSON_TERMLIST_ELEMENT);
		assertEquals("XXXTESTOwain Glyndwr",
				termList.getJSONObject(0).getString(TERM_DISPLAYNAME_ELEMENT));
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
		JSONObject data=simplePerson(name);
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);	
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
		JSONObject data=simplePerson(name);
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);		
		String url=out.getHeader("Location");
		out=tester.PUTData("/vocabularies"+url,data,jetty);	
	}
	
	private void deleteName(String name,ServletTester jetty) throws Exception
	{
		// Delete
		JSONObject data=simplePerson(name);
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",data,jetty);		
		String url=out.getHeader("Location");
		tester.DELETEData("/vocabularies/"+url,jetty);
	}
	

	
}
