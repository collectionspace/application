/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

// test comment

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// These are to test the functionality for Organization as defined in WebUI.java
public class TestOrgThroughWebapp  {
	private static final Logger log = LoggerFactory
			.getLogger(TestOrgThroughWebapp.class);
	

	private static TestBase testbase = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=testbase.setupJetty();
			}
		catch(Exception ex){
			log.error("TestOrgThroughWebapp: Could not set up jetty! " + ex.getLocalizedMessage());
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		testbase.stopJetty(jetty);
	}

	//need a begin function that creates the default person if it is missing?
	@BeforeClass public static void testCreateAuth() throws Exception {
		log.info("org_before");
		HttpTester out = null;
		JSONObject test2 = new JSONObject();
		JSONObject test = new JSONObject();
		try{
			out = testbase.GETData(TestBase.AUTHS_INIT_PATH, jetty);
			log.info(out.getContent());
		} catch(Exception ex) {
			log.warn("Exception while trying to Initialize auth vocabs: "+ex.getLocalizedMessage());
		}
		log.info("org_after");
	}
	
	/**
	 * Tests that an authority list includes the expected item difference
	 * between an authority list and a vocabulary list is: auth list lists all
	 * the vocabularies within an auth vocab list just list the one vocabulary *
	 */
	@Test
	public void testAuthoritiesSearchList() throws Exception {
		log.info("ORG : AuthoritiesList : test_start");
		// Create
		JSONObject data=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test My Authority2");
		HttpTester out = testbase.POSTData(TestBase.MAIN_ORG_INSTANCE_PATH+"/", data, jetty);
		String url = out.getHeader("Location");
		// Get List
		int resultsize = 1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found = false;
		while (resultsize > 0) {
			log.info("ORG : AuthoritiesList : Get Page: " + pagenum);
			out = testbase.GETData("/authorities/organization?pageSize=40&pageNum="
					+ pagenum, jetty);
			pagenum++;
			String content=out.getContent();
			System.err.println("XXX If this test fails shortly after this statement, there's a good chance that you don't have the default system file encoding set to UTF8."+
					"Unfortunately a library we depend on for the test rarness is badly written with respect to character set handling. We're working on it.\n");
			JSONArray results = new JSONObject(content).getJSONArray("items");

			if (results.length() == 0
					|| checkpagination.equals(results.getJSONObject(0).getString("csid"))) {
				resultsize = 0;
				// testing whether we have actually returned the same page or
				// the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");

			for (int i = 0; i < results.length(); i++) {
				JSONObject entry = results.getJSONObject(i);
				if(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("test my authority2")) {
					found = true;
					resultsize = 0;
				}
			}
		}
		assertTrue(found);
		// Delete
		testbase.DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : AuthoritiesList : test_end");
	//}
		/**
		 * Tests that an authority search includes the expected item difference
		 * between an authority search and a vocabulary search is: auth search
		 * searches all the vocabularies within an auth vocab search just searches
		 * the one vocabulary *
		 */
		//@Test
		//public void testAuthoritiesSearch() throws Exception {
		log.info("ORG : AuthoritiesSearch : test_start");
		// Create
		data=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test My Authority1");
		out = testbase.POSTData(TestBase.MAIN_ORG_INSTANCE_PATH+"/", data, jetty);
		url = out.getHeader("Location");
		// Search
		out = testbase.GETData(
				"/authorities/organization/search?query=Test+My+Authority1",
				jetty);
		//log.info(out.getContent());
		JSONArray results = new JSONObject(out.getContent())
		.getJSONArray("results");
		assertTrue(results.length() > 0);
		Boolean test = false;
		for (int i = 0; i < results.length(); i++) {
			JSONObject entry = results.getJSONObject(i);
			if(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("test my authority1")) {
				test = true;
			}
			assertEquals(entry.getString("number"),entry.getString(TERM_DISPLAYNAME_ELEMENT));
			assertTrue(entry.has("refid"));
		}
		assertTrue(test);
		// Delete
		testbase.DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : AuthoritiesSearch : test_end");
	}

	/**
	 * Tests that an vocabulary search includes the expected item difference
	 * between an authority search and a vocabulary search is: auth search
	 * searches all the vocabularies within an auth vocab search just searches
	 * the one vocabulary *
	 */
	@Test
	public void testOrganizationSearchList() throws Exception {
		log.info("ORG : OrganizationSearch : test_start");
		// Create
		JSONObject datad=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test Organization XXX");
		HttpTester outd = testbase.POSTData(TestBase.SECOND_ORG_INSTANCE_PATH+"/", datad, jetty);
		String urdl = outd.getHeader("Location");
		// Search
		//Nuxeos rebuild borks this test - lost partial matching
		//out = tester.GETData(TestBase.MAIN_ORG_INSTANCE_PATH+"/search?query=Test+Organ", jetty);
		outd = testbase.GETData(TestBase.SECOND_ORG_INSTANCE_PATH+"/search?query=Test+Organization", jetty);

		JSONArray results = new JSONObject(outd.getContent()).getJSONArray("results");

		Boolean test = false;
		for (int i = 0; i < results.length(); i++) {
			JSONObject entry = results.getJSONObject(i);
			log.info(entry.toString());
			if(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("test organization xxx")) {
				test = true;
			}
			assertEquals(entry.getString("number"),entry.getString(TERM_DISPLAYNAME_ELEMENT));
			assertTrue(entry.has("refid"));
		}
		assertTrue(test);

		// Delete
		testbase.DELETEData("/vocabularies/" + urdl, jetty);

		log.info("ORG : OrganizationSearch : test_end");
	//}

	/**
	 * Tests that a vocabularies organization list includes the expected item
	 * difference between an authority list and a vocabulary list is: auth list
	 * lists all the vocabularies within an auth vocab list just list the one
	 * vocabulary *
	 */
		//@Test
		//public void testOrganizationList() throws Exception {
		log.info("ORG : OrganizationList : test_start");
		// Create
		JSONObject data=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test my Org XXX1");
		HttpTester out = testbase.POSTData(TestBase.SECOND_ORG_INSTANCE_PATH+"/", data, jetty);
		String url = out.getHeader("Location");

		int resultsize = 1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found = false;
		while (resultsize > 0) {
			log.info("ORG : OrganizationList : GET page:" + pagenum);
			out = testbase.GETData(TestBase.SECOND_ORG_INSTANCE_PATH+"?pageSize=40&pageNum="
					+ pagenum, jetty);
			pagenum++;
			results = new JSONObject(out.getContent()).getJSONArray("items");

			if (results.length() == 0
					|| checkpagination.equals(results.getJSONObject(0).getString("csid"))) {
				resultsize = 0;
				// testing whether we have actually returned the same page or
				// the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");
			for (int i = 0; i < results.length(); i++) {
				JSONObject entry = results.getJSONObject(i);
				if(entry.getString(TERM_DISPLAYNAME_ELEMENT).toLowerCase().contains("test my org xxx1")) {
					found = true;
					resultsize = 0;
				}
			}
		}
		assertTrue(found);

		// Delete
		testbase.DELETEData("/vocabularies/" + url, jetty);

		log.info("ORG : OrganizationList : test_end");
		//}


		// Tests an Update for an Organization
		//@Test
		//public void testOrganizationCreateUpdateDelete() throws Exception {
		log.info("ORG : OrganizationCreateUpdateDelete : test_start");
		// Create
		data=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test my Org XXX4");
		out = testbase.POSTData(TestBase.MAIN_ORG_INSTANCE_PATH+"/", data, jetty);
		url = out.getHeader("Location");
		// Read
		out = testbase.GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		JSONArray termList = data.getJSONArray(ORG_TERMLIST_ELEMENT);
		assertEquals("Test my Org XXX4",
				termList.getJSONObject(0).getString(TERM_DISPLAYNAME_ELEMENT));
		// Update
		data=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "A New Test Org");
		out = testbase.PUTData("/vocabularies" + url, data, jetty);
		// Read
		out = testbase.GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		termList = data.getJSONArray(ORG_TERMLIST_ELEMENT);
		assertEquals("A New Test Org",
				termList.getJSONObject(0).getString(TERM_DISPLAYNAME_ELEMENT));
		// Delete
		testbase.DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : OrganizationCreateUpdateDelete : test_end");

	}

	/**
	 * this test will only work if you have field set up in default xml with two
	 * authorities assigned. Therefore only until default needs that behaviour
	 * this test will have to manually run don't forget to add in the instances
	 * necceassry as well.
	 * 
	 * @Test
	 */
	public void testNamesMultiAssign() throws Exception {
		log.info("ORG : NamesMultiAssign : test_start");
		// Create in single assign list:
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Custom Data'}}");
		HttpTester out = testbase.POSTData("/vocabularies/pcustom/", data, jetty);
		String url = out.getHeader("Location");
		data = new JSONObject("{'fields':{'displayName':'Custom Data 3'}}");
		out = testbase.POSTData("/vocabularies/pcustom/", data, jetty);
		String url3 = out.getHeader("Location");
		// Create in second single assign list:
		data = new JSONObject("{'fields':{'displayName':'Custom Data 2'}}");
		out = testbase.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/", data, jetty);
		String url2 = out.getHeader("Location");
		// Read
		out = testbase.GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals("Custom Data", data.getString("displayName"));

		out = testbase.GETData("/intake/autocomplete/currentOwner?q=Custom&limit=150",
				jetty);
		out = testbase.GETData("/intake/autocomplete/depositor?q=Custom&limit=150",
				jetty);

		// Delete
		testbase.DELETEData("/vocabularies/" + url, jetty);
		// Delete
		testbase.DELETEData("/vocabularies/" + url3, jetty);
		// Delete
		testbase.DELETEData("/vocabularies/" + url2, jetty);

		log.info("ORG : NamesMultiAssign : test_end");
	}

	// TODO move this to somewhere common across various tests
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
	
	// Tests both a person and an organization autocomplete for an organization
	@Test
	public void testAutocompletesForOrganization() throws Exception {
		log.info("ORG : AutocompletesForOrganization : test_start");
		// Create
		log.info("ORG : AutocompletesForOrganization : CREATE");
		JSONObject org=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test my Org XXX5");
		HttpTester out = testbase.POSTData(TestBase.MAIN_ORG_INSTANCE_PATH+"/", org, jetty);
		String url1 = out.getHeader("Location");
		// Add a person
		log.info("ORG : AutocompletesForOrganization : ADD Person");
		JSONObject person=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, "Test Auto Person");
		out = testbase.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/", person, jetty);
		String url2 = out.getHeader("Location");
		// A second organization
		log.info("ORG : AutocompletesForOrganization : Add org");
		JSONObject org2=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, "Test another Org");
		out = testbase.POSTData(TestBase.MAIN_ORG_INSTANCE_PATH+"/", org2, jetty);
		String url3 = out.getHeader("Location");

		// Test Autocomplete contactName
		log.info("ORG : AutocompletesForOrganization : test against contact Name");
		out = testbase.GETData(
				TestBase.MAIN_ORG_INSTANCE_PATH+"/autocomplete/contactName?q=Test+Auto&limit=150",
				jetty);
		JSONArray data = new JSONArray(out.getContent());
		for (int i = 0; i < data.length(); i++) {
			JSONObject entry = data.getJSONObject(i);
			JSONArray displayNames = entry.getJSONArray(DISPLAY_NAMES);
			assertTrue(displayNames.getString(0).toLowerCase().contains("test auto person"));
			assertTrue(entry.has("baseUrn"));
		}
		// Test Autocomplete subBody
		log.info("ORG : AutocompletesForOrganization : test against subBody");
		out = testbase.GETData(
				TestBase.MAIN_ORG_INSTANCE_PATH+"/autocomplete/subBody?q=Test+another&limit=150",
				jetty);
		data = new JSONArray(out.getContent());
		for (int i = 0; i < data.length(); i++) {
			JSONObject entry = data.getJSONObject(i);
			JSONArray displayNames = entry.getJSONArray(DISPLAY_NAMES);
			assertTrue(displayNames.getString(0).toLowerCase().contains("test another org"));
			assertTrue(entry.has("baseUrn"));
		}
		// Delete
		log.info("ORG : AutocompletesForOrganization : DELETE");
		testbase.DELETEData("/vocabularies/" + url1, jetty);
		testbase.DELETEData("/vocabularies/" + url2, jetty);
		testbase.DELETEData("/vocabularies/" + url3, jetty);
		log.info("ORG : AutocompletesForOrganization : test_end");
	}

	// Tests that a redirect goes to the expected place
	@Test
	public void testAutocompleteRedirect() throws Exception {
		log.info("ORG : AutocompleteRedirect : test_start");
		HttpTester out = testbase.GETData("/cataloging/source-vocab/contentOrganization",
				jetty);
		JSONArray data = new JSONArray(out.getContent());
		boolean test = false;
		for (int i = 0; i < data.length(); i++) {
			String url = data.getJSONObject(i).getString("url");
			if (url.equals(TestBase.MAIN_ORG_INSTANCE_PATH)) {
				test = true;
			}
		}
		assertTrue("correct vocab not found", test);
		log.info("ORG : AutocompleteRedirect : test_end");
	}
}
