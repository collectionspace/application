package org.collectionspace.chain.csp.persistence.services.vocab;

// test comment

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
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

// These are to test the functionality for Organization as defined in WebUI.java
public class TestOrgThroughWebapp extends TestBase {
	private static final Logger log = LoggerFactory
			.getLogger(TestOrgThroughWebapp.class);

	/**
	 * Tests that an authority search includes the expected item difference
	 * between an authority search and a vocabulary search is: auth search
	 * searches all the vocabularies within an auth vocab search just searches
	 * the one vocabulary *
	 */
	@Test
	public void testAuthoritiesSearch() throws Exception {
		log.info("ORG : AuthoritiesSearch : test_start");
		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Test My Authority1'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");
		// Search
		out = GETData(
				"/authorities/organization/search?query=Test+My+Authority1",
				jetty);
		JSONArray results = new JSONObject(out.getContent())
				.getJSONArray("results");
		assertTrue(results.length() > 0);
		Boolean test = false;
		for (int i = 0; i < results.length(); i++) {
			JSONObject entry = results.getJSONObject(i);
			log.info(entry.toString());
			if (entry.getString("displayName").toLowerCase().contains(
					"test my authority1")) {
				test = true;
			}
			assertEquals(entry.getString("number"), entry
					.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		assertTrue(test);
		// Delete
		DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : AuthoritiesSearch : test_end");
	}

	/**
	 * Tests that an authority list includes the expected item difference
	 * between an authority list and a vocabulary list is: auth list lists all
	 * the vocabularies within an auth vocab list just list the one vocabulary *
	 */
	@Test
	public void testAuthoritiesList() throws Exception {
		log.info("ORG : AuthoritiesList : test_start");
		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Test My Authority2'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");
		// Get List
		int resultsize = 1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found = false;
		while (resultsize > 0) {
			log.info("ORG : AuthoritiesList : Get Page: " + pagenum);
			out = GETData("/authorities/organization?pageSize=40&pageNum="
					+ pagenum, jetty);
			pagenum++;
			JSONArray results = new JSONObject(out.getContent())
					.getJSONArray("items");

			if (results.length() == 0
					|| checkpagination.equals(results.getJSONObject(0)
							.getString("csid"))) {
				resultsize = 0;
				// testing whether we have actually returned the same page or
				// the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");

			for (int i = 0; i < results.length(); i++) {
				JSONObject entry = results.getJSONObject(i);
				if (entry.getString("displayName").toLowerCase().contains(
						"test my authority2")) {
					found = true;
					resultsize = 0;
				}
			}
		}
		assertTrue(found);
		// Delete
		DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : AuthoritiesList : test_end");
	}

	/**
	 * Tests that an vocabulary search includes the expected item difference
	 * between an authority search and a vocabulary search is: auth search
	 * searches all the vocabularies within an auth vocab search just searches
	 * the one vocabulary *
	 */
	@Test
	public void testOrganizationSearch() throws Exception {
		log.info("ORG : OrganizationSearch : test_start");
		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Test Organization XXX'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");
		// Search
		out = GETData("/vocabularies/organization/search?query=Test+Organ",
				jetty);

		JSONArray results = new JSONObject(out.getContent())
				.getJSONArray("results");

		Boolean test = false;
		for (int i = 0; i < results.length(); i++) {
			JSONObject entry = results.getJSONObject(i);
			log.info(entry.toString());
			if (entry.getString("displayName").toLowerCase().contains(
					"test organization xxx")) {
				test = true;
			}
			assertEquals(entry.getString("number"), entry
					.getString("displayName"));
			assertTrue(entry.has("refid"));
		}
		assertTrue(test);

		// Delete
		DELETEData("/vocabularies/" + url, jetty);

		log.info("ORG : OrganizationSearch : test_end");
	}

	/**
	 * Tests that a vocabularies organization list includes the expected item
	 * difference between an authority list and a vocabulary list is: auth list
	 * lists all the vocabularies within an auth vocab list just list the one
	 * vocabulary *
	 */
	@Test
	public void testOrganizationList() throws Exception {
		log.info("ORG : OrganizationList : test_start");

		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Test my Org XXX1'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");

		int resultsize = 1;
		int pagenum = 0;
		String checkpagination = "";
		boolean found = false;
		while (resultsize > 0) {
			log.info("ORG : OrganizationList : GET page:" + pagenum);
			out = GETData("/vocabularies/organization?pageSize=40&pageNum="
					+ pagenum, jetty);
			pagenum++;
			JSONArray results = new JSONObject(out.getContent())
					.getJSONArray("items");

			if (results.length() == 0
					|| checkpagination.equals(results.getJSONObject(0)
							.getString("csid"))) {
				resultsize = 0;
				// testing whether we have actually returned the same page or
				// the next page - all csid returned should be unique
			}
			checkpagination = results.getJSONObject(0).getString("csid");
			for (int i = 0; i < results.length(); i++) {
				JSONObject entry = results.getJSONObject(i);
				if (entry.getString("displayName").toLowerCase().contains(
						"test my org xxx1")) {
					found = true;
					resultsize = 0;
				}
			}
		}
		assertTrue(found);

		// Delete
		DELETEData("/vocabularies/" + url, jetty);

		log.info("ORG : OrganizationList : test_end");
	}

	// Tests a READ for an organization
	@Test
	public void testOrganizationGet() throws Exception {
		log.info("ORG : OrganizationGet : test_start");
		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'TestmyOrgXXX2'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");
		// Search
		out = GETData("/vocabularies/organization/search?query=TestmyOrgXXX2",
				jetty);

		// Find candidate
		JSONArray results = new JSONObject(out.getContent())
				.getJSONArray("results");
		log.info(Integer.toString(results.length()));

		assertTrue(results.length() > 0);
		JSONObject entry = results.getJSONObject(0);
		String csid = entry.getString("csid");
		out = GETData("/vocabularies/organization/" + csid, jetty);
		JSONObject fields = new JSONObject(out.getContent())
				.getJSONObject("fields");
		assertEquals(csid, fields.getString("csid"));
		assertEquals("TestmyOrgXXX2", fields.getString("displayName"));

		// Delete
		DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : OrganizationGet : test_end");
	}

	// Tests an Update for an Organization
	@Test
	public void testOrganizationCreateUpdateDelete() throws Exception {
		log.info("ORG : OrganizationCreateUpdateDelete : test_start");
		ServletTester jetty = setupJetty();
		// Create
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Test my Org XXX4'}}");
		HttpTester out = POSTData("/vocabularies/organization/", data, jetty);
		String url = out.getHeader("Location");
		// Read
		out = GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals("Test my Org XXX4", data.getString("displayName"));
		// Update
		data = new JSONObject("{'fields':{'displayName':'A New Test Org'}}");
		out = PUTData("/vocabularies" + url, data, jetty);
		// Read
		out = GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals("A New Test Org", data.getString("displayName"));
		// Delete
		DELETEData("/vocabularies/" + url, jetty);
		log.info("ORG : OrganizationCreateUpdateDelete : test_end");

	}

	/*
	 * this test will only work if you have field set up in default xml with two
	 * authorities assigned. Therefore only until default needs that behaviour
	 * this test will have to manually run don't forget to add in the instances
	 * necceassry as well.
	 * 
	 * @Test
	 */
	public void testNamesMultiAssign() throws Exception {
		log.info("ORG : NamesMultiAssign : test_start");
		ServletTester jetty = setupJetty();
		// Create in single assign list:
		JSONObject data = new JSONObject(
				"{'fields':{'displayName':'Custom Data'}}");
		HttpTester out = POSTData("/vocabularies/pcustom/", data, jetty);
		String url = out.getHeader("Location");
		data = new JSONObject("{'fields':{'displayName':'Custom Data 3'}}");
		out = POSTData("/vocabularies/pcustom/", data, jetty);
		String url3 = out.getHeader("Location");
		// Create in second single assign list:
		data = new JSONObject("{'fields':{'displayName':'Custom Data 2'}}");
		out = POSTData("/vocabularies/person/", data, jetty);
		String url2 = out.getHeader("Location");
		// Read
		out = GETData("/vocabularies" + url, jetty);
		data = new JSONObject(out.getContent()).getJSONObject("fields");
		assertEquals(data.getString("csid"), url.split("/")[2]);
		assertEquals("Custom Data", data.getString("displayName"));

		out = GETData("/intake/autocomplete/currentOwner?q=Custom&limit=150",
				jetty);
		String one = out.getContent();
		out = GETData("/intake/autocomplete/depositor?q=Custom&limit=150",
				jetty);
		String two = out.getContent();

		// Delete
		DELETEData("/vocabularies/" + url, jetty);
		// Delete
		DELETEData("/vocabularies/" + url3, jetty);
		// Delete
		DELETEData("/vocabularies/" + url2, jetty);

		log.info("ORG : NamesMultiAssign : test_end");
	}

	// Tests both a person and an organization autocomplete for an organization
	@Test
	public void testAutocompletesForOrganization() throws Exception {
		log.info("ORG : AutocompletesForOrganization : test_start");
		ServletTester jetty = setupJetty();
		// Create
		log.info("ORG : AutocompletesForOrganization : CREATE");
		JSONObject org = new JSONObject(
				"{'fields':{'displayName':'Test my Org XXX5'}}");
		HttpTester out = POSTData("/vocabularies/organization/", org, jetty);
		String url1 = out.getHeader("Location");
		// Add a person
		log.info("ORG : AutocompletesForOrganization : ADD Person");
		JSONObject person = new JSONObject(
				"{'fields':{'displayName':'Test Auto Person'}}");
		out = POSTData("/vocabularies/person/", person, jetty);
		String url2 = out.getHeader("Location");
		// A second organization
		log.info("ORG : AutocompletesForOrganization : Add org");
		JSONObject org2 = new JSONObject(
				"{'fields':{'displayName':'Test another Org'}}");
		out = POSTData("/vocabularies/organization/", org2, jetty);
		String url3 = out.getHeader("Location");

		// Test Autocomplete contactName
		log
				.info("ORG : AutocompletesForOrganization : test against contact Name");
		out = GETData(
				"/vocabularies/organization/autocomplete/contactName?q=Test+Auto&limit=150",
				jetty);
		JSONArray data = new JSONArray(out.getContent());
		for (int i = 0; i < data.length(); i++) {
			JSONObject entry = data.getJSONObject(i);
			assertTrue(entry.getString("label").toLowerCase().contains(
					"test auto person"));
			assertTrue(entry.has("urn"));
		}
		// Test Autocomplete subBody
		log.info("ORG : AutocompletesForOrganization : test against subBody");
		out = GETData(
				"/vocabularies/organization/autocomplete/subBody?q=Test+another&limit=150",
				jetty);
		data = new JSONArray(out.getContent());
		for (int i = 0; i < data.length(); i++) {
			JSONObject entry = data.getJSONObject(i);
			;
			assertTrue(entry.getString("label").toLowerCase().contains(
					"test another org"));
			assertTrue(entry.has("urn"));
		}
		// Delete
		log.info("ORG : AutocompletesForOrganization : DELETE");
		DELETEData("/vocabularies/" + url1, jetty);
		DELETEData("/vocabularies/" + url2, jetty);
		DELETEData("/vocabularies/" + url3, jetty);
		log.info("ORG : AutocompletesForOrganization : test_end");
	}

	// Tests that a redirect goes to the expected place
	@Test
	public void testAutocompleteRedirect() throws Exception {
		log.info("ORG : AutocompleteRedirect : test_start");
		ServletTester jetty = setupJetty();

		HttpTester out = GETData("/objects/source-vocab/contentOrganization",
				jetty);
		JSONArray data = new JSONArray(out.getContent());
		boolean test = false;
		for (int i = 0; i < data.length(); i++) {
			String url = data.getJSONObject(i).getString("url");
			if (url.equals("/vocabularies/organization")) {
				test = true;
			}
		}
		assertTrue("correct vocab not found", test);
		log.info("ORG : AutocompleteRedirect : test_end");

	}
}
