/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnUnknown;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;

public class TestService extends ServicesBaseClass {
	private static final Logger log = LoggerFactory
			.getLogger(TestService.class);

	@Before
	public void checkServicesRunning() throws ConnectionException {
		setup();
	}

	@Test
	public void testAssumptionMechanism() {
		log.debug("Services Running!");
	}

	protected JSONObject getJSON(String in) throws IOException, JSONException {
		String path = getClass().getPackage().getName().replaceAll("\\.", "/");
		InputStream stream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(path + "/" + in);

		assertNotNull("missing file:" + in, stream);
		String data = IOUtils.toString(stream, "UTF-8");
		stream.close();
		return new JSONObject(data);
	}

	@Test
	public void testXMLJSONConversion() throws Exception {

		CSPManager cspm = new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		// argh - test break when config changes *sob*
		cspm.configure(getRootSource(), new ConfigFinder(null),false);
		ConfigRoot root = cspm.getConfigRoot();
		Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);

		testXMLJSON(spec, "location", "location.xml", "location.json");
		testXMLJSON(spec, "concept", "concept.xml", "concept.json");
                testXMLJSON(spec, "place", "placeXMLJSON.xml", "placeJSON.json");
                testXMLJSON(spec, "citation", "citation.xml", "citation.json");
        testXMLJSON(spec, "work", "work.xml", "work.json");
		testXMLJSON(spec, "collection-object", "objectsXMLJSON.xml",
				"objectsJSON.json");

		testXMLJSON(spec, "acquisition", "acquisitionXMLJSON.xml",
				"acquisitionJSON.json");
		testXMLJSON(spec, "intake", "intake.xml", "intake.json");
		testXMLJSON(spec, "loanin", "loanin.xml", "loanin.json");
		testXMLJSON(spec, "loanout", "loanout.xml", "loanout.json");
		testXMLJSON(spec, "valuationcontrol", "valuationcontrol.xml", "valuationcontrol.json");
		testXMLJSON(spec, "movement", "movement.xml", "movement.json");
		testXMLJSON(spec, "objectexit", "objectexit.xml", "objectexit.json");
		testXMLJSON(spec, "group", "group.xml", "group.json");
		testXMLJSON(spec, "media", "media.xml", "mediaJSON.json");
		testXMLJSON(spec, "conditioncheck", "conditioncheck.xml", "conditioncheck.json");
		testXMLJSON(spec, "conservation", "conservation.xml", "conservation.json");
		testXMLJSON(spec, "exhibition", "exhibition.xml", "exhibition.json");

		testXMLJSON(spec, "role", "role.xml", "role.json");
		testXMLJSON(spec, "permrole", "rolepermissions.xml",
				"rolepermissions.json");
		testXMLJSON(spec, "userrole", "accountrole.xml", "accountrole.json");

		// testXMLJSON(spec,
		// "permission","permissionXMLJSON.xml","permissionsJSON.json");
		// testXMLJSON(spec,
		// "organization","orgauthref.xml","permissionsJSON.json");
	}

	/**
	 * I wouldn't call this a robust multipart test - needs more work but works
	 * fine for single part multipart xml
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJSONXMLConversion() throws Exception {

		CSPManager cspm = new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		// argh - test break when config changes *sob*
		cspm.configure(getRootSource(),new ConfigFinder(null),false);
		ConfigRoot root = cspm.getConfigRoot();
		Spec spec = (Spec) root.getRoot(Spec.SPEC_ROOT);

		testJSONXML(spec, "location", "location.xml", "location.json");
		testJSONXML(spec, "concept", "concept.xml", "concept.json");
		testJSONXML(spec, "citation", "citation.xml", "citation.json");
                testJSONXML(spec, "place", "placeXMLJSON.xml", "placeJSON.json");
        testJSONXML(spec, "work", "work.xml", "work.json");
        // CSPACE-6135: In CollectionObject, the computedCurrentLocation field is services-readonly,
        // so the JSON->XML->JSON conversion produces JSON that does not match the initial JSON
        // (computedCurrentLocation is omitted from the XML, so it does not appear in the JSON
        // converted back from the XML). In this case, we need to supply a third parameter to
        // specify the expected round-trip JSON. objectsReturnedJSON.json is identical to
        // objectsJSON.json, except computedCurrentLocation has been removed.
		testJSONXML(spec, "collection-object", "objectsXMLJSON.xml",
				"objectsJSON.json", "objectsReturnedJSON.json");
		
		testJSONXML(spec, "acquisition", "acquisitionXMLJSON.xml",
		 		"acquisitionJSON.json");

		testJSONXML(spec, "media", "media.xml", "mediaJSON.json");
		testJSONXML(spec, "loanin", "loanin.xml", "loanin.json");
		testJSONXML(spec, "loanout", "loanout.xml", "loanout.json");
		testJSONXML(spec, "intake", "intake.xml", "intake.json");
		testJSONXML(spec, "movement", "movement.xml", "movement.json");
		testJSONXML(spec, "valuationcontrol", "valuationcontrol.xml", "valuationcontrol.json");
		testJSONXML(spec, "objectexit", "objectexit.xml", "objectexit.json");
		testJSONXML(spec, "group", "group.xml", "group.json");
		testJSONXML(spec, "conditioncheck", "conditioncheck.xml", "conditioncheck.json");
		testJSONXML(spec, "conservation", "conservation.xml", "conservation.json");
		testJSONXML(spec, "exhibition", "exhibition.xml", "exhibition.json");

		testJSONXML(spec, "role", "role.xml", "role.json");

		// testJSONXML(spec,"permrole","rolepermissions.xml","rolepermissions.json");
		// testJSONXML(spec, "userrole","accountrole.xml","accountrole.json");
		// testJSONXML(spec,
		// "permission","permissionXMLJSON.xml","permissionsJSON.json");
	}
		
	/**
	 * Tests conversion of a JSON file to XML. This implementation does not compare the
	 * resultant generated XML to an expected XML file. Instead, it converts the resultant
	 * XML back into JSON, and compares that round-trip JSON to an expected JSON file.
	 * 
	 * @param spec
	 * @param objtype
	 * @param xmlfile			Name of the file containing the expected XML (not currently used)
	 * @param jsonfile			Name of the file containing JSON to be converted to XML
	 * @param returnedjsonfile	Name of the file containing the expected round-trip JSON, converted back from XML 
	 * @throws Exception
	 */
	private void testJSONXML(Spec spec, String objtype, String xmlfile,
			String jsonfile, String returnedjsonfile) throws Exception {

		log.info("Converting JSON to XML for record type " + objtype);
		Record r = spec.getRecord(objtype);
		JSONObject j = getJSON(jsonfile);
		//log.info("Original JSON:\n" + j.toString());
		Map<String, Document> parts = new HashMap<String, Document>();
		Document doc = null;
		JSONObject testjson = new JSONObject();
		for (String section : r.getServicesRecordPathKeys()) {
			if (section.equals("common")) {
				String path = r.getServicesRecordPath(section);
				String[] record_path = path.split(":", 2);
				doc = XmlJsonConversion.convertToXml(r, j, section, "");
				parts.put(record_path[0], doc);
				//log.info("After JSON->XML conversion:\n" + doc.asXML());
				JSONObject repeatjson = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion
						.convertToJson(r, doc, "", "common","","");// this is where we
																// specify the
																// multipart
																// section
				// we are considering
				for (String name : JSONObject.getNames(repeatjson)) {
					testjson.put(name, repeatjson.get(name));
				}
				//log.info("After XML->JSON re-conversion:\n" + testjson.toString());
			}
		}
		// convert json -> xml and back to json and see if it still looks the
		// same..
		JSONObject expectedjson = getJSON(returnedjsonfile);
		boolean result = JSONUtils.checkJSONEquivOrEmptyStringKey(expectedjson,testjson);
		if(!result) {
			log.info("Original JSON:\n" + j.toString());
			log.info("After JSON->XML conversion:\n" + doc.asXML());
			log.info("After XML->JSON re-conversion:\n" + testjson.toString());
		}
		assertTrue("JSON->XML->JSON round-trip for record type: "+objtype+" doesn't match original JSON",
				result);

	}


	/**
	 * Tests conversion of a JSON file to XML, comparing the round-trip JSON to the original JSON.
	 */
	private void testJSONXML(Spec spec, String objtype, String xmlfile,
			String jsonfile) throws Exception {
		testJSONXML(spec, objtype, xmlfile, jsonfile, jsonfile);
	}
	
	/**
	 * This doesn't currently test multipart xml conversion
	 * 
	 * @param spec
	 * @param objtype
	 * @param xmlfile
	 * @param jsonfile
	 * @throws Exception
	 */
	private void testXMLJSON(Spec spec, String objtype, String xmlfile,
			String jsonfile) throws Exception {

		log.info("Converting XML to JSON for record type " + objtype);
		Document testxml = getDocument(xmlfile);
		String test = testxml.asXML();
		log.trace("Original XML:\n" + test);
		JSONObject j = getJSON(jsonfile);
		//log.info("Original JSON:\n" + j.toString());
		Record r = spec.getRecord(objtype);
		JSONObject repeatjson = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion
				.convertToJson(r, testxml, "", "common","",""); // this is where we specify the multipart section
															// we are considering
		boolean result = JSONUtils.checkJSONEquivOrEmptyStringKey(repeatjson, j);
		if(!result) {
			log.info("Original JSON:\n" + j.toString());
			log.info("After XML->JSON conversion:\n" + repeatjson.toString());
		}
		assertTrue("Generated JSON for record type: "+objtype+" doesn't match original JSON",result );

	}

	@Test 
	public void testPersonContactPostViaCSIDs() throws Exception {
		String filename = "";
		String partname = "";
		ReturnedURL url = null;
		Map<String, Document> parts = new HashMap<String, Document>();
		StringBuilder serviceurl = new StringBuilder("");
		ReturnedMultipartDocument rdocs = null;
		ReturnedDocument rdoc = null;
		int status = 0;
		Document doc = null;
		String text = "";
		String xpath = "";

		// POST (Create) a person authority
		serviceurl.append("personauthorities/");
		partname = "personauthorities_common";
		filename = "personAuth.xml";
		log.info("Testing create at " + serviceurl + " with " + filename
				+ " and partname=" + partname);
		parts.put(partname, getDocument(filename));
		url = conn.getMultipartURL(RequestMethod.POST, serviceurl.toString(),
				parts, creds, cache);
		assertEquals(201, url.getStatus());
		String authUrl = url.getURL();
		String authId = url.getURLTail();
		// Test creation with a GET
		if (partname != null) {
			rdocs = conn.getMultipartXMLDocument(RequestMethod.GET, authUrl,
					null, creds, cache);
			status = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		}
		assertEquals(200, status);
		assertNotNull(doc);

		log.info("CREATED PERSONAUTHORITY AT " + authUrl);

		// POST (Create) a person item within the person authority
		serviceurl.append(authId + "/items/");
		partname = "persons_common";
		String partname1 = "relations-common-list";
		filename = "personItem.xml";
		String filename1 = "relationshipItem.xml";
		log.info("Testing create at " + serviceurl + " with " + filename
				+ " and partname=" + partname);
		if (partname != null) {
			parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(filename));
	//		parts.put(partname1, getDocument(filename1));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl
					.toString(), parts, creds, cache);
		} 
		assertEquals(201, url.getStatus());
		String itemUrl = url.getURL();
		String itemId = url.getURLTail();
		// Test creation with a GET
		if (partname != null) {
			rdocs = conn.getMultipartXMLDocument(RequestMethod.GET, itemUrl,
					null, creds, cache);
			status = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} 
		assertEquals(200, status);
		assertNotNull(doc);

		// Test that the parent authority lists this item as a child
		String parentUrl = authUrl + "/items/";
		log.info("LIST from " + parentUrl);
		rdoc = conn.getXMLDocument(RequestMethod.GET, parentUrl, null, creds,
				cache);
		status = rdoc.getStatus();
		doc = rdoc.getDocument();
		assertEquals(200, status);
		log.info(doc.asXML());
		xpath = "//totalItems";
		Node n = doc.selectSingleNode(xpath);
		assertNotNull(n);
		text = n.getText();
		assertNotNull(text);
		log.info("Value of XPath expression '" + xpath + "' = " + text);
		assert (!text.trim().equals("0"));
		xpath = "//list-item/csid";
		List<Node> nodes = doc.selectNodes(xpath);
		assertNotNull(nodes);
		assert (nodes.size() > 0);
		boolean foundItemInAuthority = false;
		for (Node node : nodes) {
			log.info("found '" + node.getText().trim() + "' comparing to "
					+ itemId);
			if (node.getText().trim().equals(itemId)) {
				foundItemInAuthority = true;
			}
		}
		assert (foundItemInAuthority);
		log.info("CREATED PERSON AT " + itemUrl);

		// POST (Create) a contact sub-resource within the person item
		// and perform a full POST, GET, DELETE cycle on that contact
		serviceurl.append(itemId + "/contacts");
		partname = "contacts_common";
		filename = "personItemContact.xml";
		log.info("ADDING CONTACT USING THIS URL " + serviceurl);
		testPostGetDelete(serviceurl.toString(), partname, filename,
				"contacts_common/emailGroupList/emailGroup/email", "test@example.com");
		// DELETE (Delete) the person item within the person authority
		status = conn
				.getNone(RequestMethod.DELETE, itemUrl, null, creds, cache);
		assertEquals(200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn
				.getNone(RequestMethod.DELETE, itemUrl, null, creds, cache);
		assertEquals(404, status);
		// GET once more to make sure it isn't there
		if (partname != null) {
			rdocs = conn.getMultipartXMLDocument(RequestMethod.GET, itemUrl,
					null, creds, cache);
			status = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} 
		assertEquals(404, status); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
		log.info("DELETED PERSON");

		// DELETE (Delete) the person authority
		status = conn
				.getNone(RequestMethod.DELETE, authUrl, null, creds, cache);
		assertEquals(200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn
				.getNone(RequestMethod.DELETE, authUrl, null, creds, cache);
		assertEquals(404, status);
		// GET once more to make sure it isn't there
		if (partname != null) {
			rdocs = conn.getMultipartXMLDocument(RequestMethod.GET, authUrl,
					null, creds, cache);
			status = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} 
		assertEquals(404, status); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
		log.info("DELETED PERSON AUTHORITY");

	}
	
	@Test
	public void testNewPersonAuthority() throws Exception {
		String personAuthFile = "personAuth.xml";
		String personAuthItemFile = "personAuthItem.xml";
		String personAuthpartname = "personauthorities_common";
		String personAuthServiceUrl = "personauthorities/";
		Map<String, Document> parts = new HashMap<String, Document>();
		parts.put(personAuthpartname, getDocument(personAuthFile));

		log.info("Testing create at " + personAuthServiceUrl + " with " + personAuthFile
				+ " and partname=" + personAuthpartname);

		ReturnedURL url = conn.getMultipartURL(RequestMethod.POST, personAuthServiceUrl,
				parts, creds, cache);
		assertEquals(201, url.getStatus());

		String id = url.getURLTail();
		log.info(url.getURL());

		String personAuthUrl = url.getURL();
		// Test creation with a GET

		ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(RequestMethod.GET, personAuthUrl,
					null, creds, cache);
		int status = rdocs.getStatus();
		assertEquals(200, status);

		Document doc = rdocs.getDocument(personAuthpartname);
		log.info(doc.asXML());

		// DELETE
		conn.getNone(RequestMethod.DELETE, "personauthorities/" + id, null,
				creds, cache);
	}

	

	//@Test
	public void testPersonContact() throws Exception {
		String serviceurl = "personauthorities/urn:cspace:name(person)/items";
		String filename = "personItem.xml";
		String partname = "persons_common";
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename
				+ " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require
		// uniqueness (to maintain self-contained tests that don't destroy
		// existing data)

		// POST (Create)
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(filename));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl, parts,
					creds, cache);
		} else {
			url = conn.getURL(RequestMethod.POST, serviceurl,
					getDocument(filename), creds, cache);
		}

		assertEquals(201, url.getStatus());

		// log.info("CREATE PERSON" + url.getURL());
		// assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g.
		// CSPACE-305 hasn't regressed
		log.info("CREATE PERSON" + url.getURL());
		// create contact person

		String serviceurlContact = "personauthorities/urn:cspace:name(person)/items/"
				+ url.getURLTail() + "/contacts";
		String filenameContact = "personItemContact.xml";
		String partnameContact = "contacts_common";
		log.info("ADD CONTACT USING THIS URL " + serviceurlContact);

		testPostGetDelete(serviceurlContact, partnameContact, filenameContact,
				"contacts_common/email", "email@example.com");

		// DELETE (Delete)
		int status = conn.getNone(RequestMethod.DELETE, url.getURL(), null,
				creds, cache);
		assertEquals(200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn.getNone(RequestMethod.DELETE, url.getURL(), null, creds,
				cache);
		assertEquals(404, status);

		log.info("DELETE PERSON");
		// GET once more to make sure it isn't there
		int getStatus;
		Document doc;
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
	}

	//@Test
	public void testOrgContact() throws Exception {
		String serviceurl = "orgauthorities/urn:cspace:name(organization)/items";
		String filename = "orgItem.xml";
		String partname = "organizations_common";
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename
				+ " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require
		// uniqueness (to maintain self-contained tests that don't destroy
		// existing data)

		// POST (Create)
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(filename));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl, parts,
					creds, cache);
		} else {
			url = conn.getURL(RequestMethod.POST, serviceurl,
					getDocument(filename), creds, cache);
		}

		assertEquals(201, url.getStatus());

		// doesn't work because name urn gets translated to id
		// assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g.
		// CSPACE-305 hasn't regressed
		log.info("CREATE ORG" + url.getURL());
		// create contact person

		String serviceurlContact = "orgauthorities/urn:cspace:name(organization)/items/"
				+ url.getURLTail() + "/contacts";
		String filenameContact = "personItemContact.xml";
		String partnameContact = "contacts_common";
		log.info("ADD CONTACT USING THIS URL " + serviceurlContact);

		testPostGetDelete(serviceurlContact, partnameContact, filenameContact,
				"contacts_common/email", "email@example.com");

		// DELETE (Delete)
		int status = conn.getNone(RequestMethod.DELETE, url.getURL(), null,
				creds, cache);
		assertEquals(200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn.getNone(RequestMethod.DELETE, url.getURL(), null, creds,
				cache);
		assertEquals(404, status);

		log.info("DELETE ORG");
		// GET once more to make sure it isn't there
		int getStatus;
		Document doc;
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
	}

	//@Test 
	// remove test as never know if all the bits for the report are there to test
	public void testReporting() throws Exception {

		ReturnedURL url;
		int getStatus;
		Document doc;
		String serviceurl = "acquisitions/";
		String partname = "acquisitions_common";
		String filename = "acquisitionXMLJSON.xml";
		String xpath = "acquisitions_common/accessionDate";
		String expected = "April 1, 2010";

		// POST (Create Acquisition Record)
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(filename));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl, parts,
					creds, cache);
		} else {
			url = conn.getURL(RequestMethod.POST, serviceurl,
					getDocument(filename), creds, cache);
		}

		assertEquals("Failed to receive 201 status code on create", 201, url
				.getStatus());
		
		//find report

		ReturnedDocument doc3 = conn.getXMLDocument(RequestMethod.GET,
				"reports?doctype=Acquisition", null, creds, cache);
		assertEquals(200, doc3.getStatus());
		Set<String> csids = new HashSet<String>();
		String reportcsid = "";
		for (Node n : (List<Node>) doc3.getDocument().selectNodes("abstract-common-list/list-item/csid")) {
			reportcsid = n.getText();
		}
		assertFalse("No Acquisition report to test with", reportcsid.equals(""));
		if(!reportcsid.equals("")){
//only runs if there is a report to run
			String reportsurl = "reports/"+reportcsid;

			String csid = url.getURLTail();
			Document report = getDocument("reportrun.xml");
			report.selectSingleNode("invocationContext/singleCSID").setText(csid);

			//DO REPORT
			//run report
			ReturnUnknown doc2 = conn.getReportDocument(RequestMethod.POST, reportsurl, report, creds, cache);
			JSONObject out = new JSONObject();
			out.put("getByteBody", doc2.getBytes());
			out.put("contenttype", doc2.getContentType());
			

			assertEquals("Failed to receive 200 status code on create", 200, doc2
					.getStatus());
		}
		
		
		// DELETE (Delete Acquisition)
		int status = conn.getNone(RequestMethod.DELETE, url.getURL(), null,
				creds, cache);
		assertEquals("Failed to receive expected 200 status code on delete",
				200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn.getNone(RequestMethod.DELETE, url.getURL(), null, creds,
				cache);
		assertEquals(
				"Failed to receive expected 404 status code on repeated delete of same record",
				404, status);

		log.info("DELETE");
		// GET once more to make sure it isn't there
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(
				"Failed to receive expected 404 status code on repeated delete of same record",
				404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull("Contents of deleted record were unexpectedly not null", doc);
	}
	@Test
	public void testAllPostGetDelete() throws Exception {
		// TODO Add vocab (the previous testVocabPost method was just an exact
		// copy of testRolesPost!)
		// TODO Add everything from CSPACE-1876 and more
		// testPostGetDelete("collectionobjects/", "collectionobjects_common",
		// "objectCreate.xml", "collectionobjects_common/objectNumber", "2");
		
		testPostGetDelete("media/", "media_common", "media.xml",
				"media_common/title", "EX2011.5");
		testCRUD("collectionobjects/", "collectionobjects_common",
				"objectCreate.xml", "objectsXMLJSON.xml",
				"collectionobjects_common/objectNumber", "2010.1.9");

		//testPostGetDelete("acquisitions/", "acquisitions_common",
		//		"acquisitionXMLJSON.xml", "acquisitions_common/accessionDate",
		//		"2010-04-01T04:00:00Z");
		testPostGetDelete("intakes/", "intakes_common", "intake.xml",
				"intakes_common/entryNumber", "IN2010.337");
		testPostGetDelete("loansin/", "loansin_common", "loanin.xml",
				"loansin_common/loanInNumber", "LI2010.1.21");
		testPostGetDelete("loansout/", "loansout_common", "loanout.xml",
				"loansout_common/loanOutNumber", "LO2010.117");
		testPostGetDelete("movements/", "movements_common", "movement.xml",
				"movements_common/movementReferenceNumber", "MV2010.99");
		testPostGetDelete("valuationcontrols/", "valuationcontrols_common", "valuationcontrol.xml",
				"valuationcontrols_common/valuationcontrolRefNumber", "VC2013.4.22");
		testPostGetDelete("objectexit/", "objectexit_common", "objectexit.xml",
				"objectexit_common/exitNumber", "EX2011.5");
		testPostGetDelete("groups/", "groups_common", "group.xml",
				"groups_common/title", "This is my group");
		testPostGetDelete("conditionchecks/", "conditionchecks_common", "conditioncheck.xml",
				"conditionchecks_common/conditionCheckRefNumber", "CC2013.001");
		testPostGetDelete("conservation/", "conservation_common", "conservation.xml",
				"conservation_common/conservationNumber", "CT2015.1");
		testPostGetDelete("exhibitions/", "exhibitions_common", "exhibition.xml",
				"exhibitions_common/exhibitionNumber", "EX123");

		// testPostGetDelete("relations/", "relations_common",
		// "relationship.xml", "relations_common/relationshipType", "affects");


		// TODO make roleName dynamically vary otherwise POST fails if already
		// exists (something like buildObject)
		// testPostGetDelete("authorization/roles/", null, "role.xml",
		// "role/description", "this role is for test users");
		// testPostGetDelete("authorization/permissions/", null,
		// "permissions.xml", "permission/resourceName", "testthing");

		// TODO might be worth adding test for CSPACE-1947 (POST with wrong
		// label "succeeds")
	}

	// TODO merge this method with testPostGetDelete - this is Chris's temporary
	// fork to help testing repeatable fields
	/**
	 * Test Create, Update, Read and Delete for a record type
	 * 
	 * @param serviceurl
	 * @param partname
	 * @param Createfilename
	 * @param Updatefilename
	 * @param xpath
	 * @param expected
	 */
	private void testCRUD(String serviceurl, String partname,
			String Createfilename, String Updatefilename, String xpath,
			String expected) throws Exception {
		ReturnedURL url;

		log.info("Testing " + serviceurl + " with " + Createfilename
				+ " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require
		// uniqueness (to maintain self-contained tests that don't destroy
		// existing data)

		// POST (Create)
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(Createfilename));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl, parts,
					creds, cache);
		} else {
			url = conn.getURL(RequestMethod.POST, serviceurl,
					getDocument(Createfilename), creds, cache);
		}

		assertEquals(201, url.getStatus());

		assertTrue(url.getURL().startsWith("/" + serviceurl)); // ensures e.g.
																// CSPACE-305
																// hasn't
																// regressed

		// GET (Read)
		int getStatus;
		Document doc;
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(200, getStatus);
		assertNotNull(doc);
		Node n = doc.selectSingleNode(xpath);
		assertNotNull(n);
		String text = n.getText();
		assertEquals(expected, text);
		// Update
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(Updatefilename));
			conn.getMultipartXMLDocument(RequestMethod.PUT, url.getURL(),
					parts, creds, cache);
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			conn.getXMLDocument(RequestMethod.PUT, url.getURL(),
					getDocument(Updatefilename), creds, cache);
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}

		assertEquals(200, getStatus);
		assertNotNull(doc);
		n = doc.selectSingleNode(xpath);
		assertNotNull(n);
		text = n.getText();
		assertEquals(expected, text);

		//log.info(doc.asXML());
		// Get

		// DELETE (Delete)
		int status = conn.getNone(RequestMethod.DELETE, url.getURL(), null,
				creds, cache);
		assertEquals(200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn.getNone(RequestMethod.DELETE, url.getURL(), null, creds,
				cache);
		assertEquals(404, status);

		// GET once more to make sure it isn't there
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
	}

	private void testPostGetDelete(String serviceurl, String partname,
			String filename, String xpath, String expected) throws Exception {
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename
				+ " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require
		// uniqueness (to maintain self-contained tests that don't destroy
		// existing data)

		// POST (Create)
		if (partname != null) {
			Map<String, Document> parts = new HashMap<String, Document>();
			parts.put(partname, getDocument(filename));
			url = conn.getMultipartURL(RequestMethod.POST, serviceurl, parts,
					creds, cache);
		} else {
			url = conn.getURL(RequestMethod.POST, serviceurl,
					getDocument(filename), creds, cache);
		}

		assertEquals("Failed to receive 201 status code on create", 201, url
				.getStatus());

		// assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g.
		// CSPACE-305 hasn't regressed

		log.info("CREATED RECORD " + url.getURL());
		// GET (Read)
		int getStatus;
		Document doc;
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals("Failed to receive expected 200 status code on read", 200,
				getStatus);
		log.trace("RETRIEVED RECORD " + doc.asXML());
		assertNotNull("Record received on read was unexpectedly null", doc);
		Node n = doc.selectSingleNode(xpath);
		assertNotNull("Expected XPath expression was not found in record", n);
		String text = n.getText();
		assertEquals("Expected value was not found in record", expected, text);

		// List
		log.info("LIST from " + serviceurl);
		ReturnedDocument rdoc1 = conn.getXMLDocument(RequestMethod.GET, "/"
				+ serviceurl, null, creds, cache);
		getStatus = rdoc1.getStatus();
		doc = rdoc1.getDocument();

		assertEquals("Failed to receive expected 200 status code on list read",
				200, getStatus);
		log.trace("LISTLISTLIST");
		log.trace(doc.asXML());
		log.trace("LISTLISTLIST");

		// DELETE (Delete)
		int status = conn.getNone(RequestMethod.DELETE, url.getURL(), null,
				creds, cache);
		assertEquals("Failed to receive expected 200 status code on delete",
				200, status);
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status = conn.getNone(RequestMethod.DELETE, url.getURL(), null, creds,
				cache);
		assertEquals(
				"Failed to receive expected 404 status code on repeated delete of same record",
				404, status);

		log.info("DELETE");
		// GET once more to make sure it isn't there
		if (partname != null) {
			ReturnedMultipartDocument rdocs = conn.getMultipartXMLDocument(
					RequestMethod.GET, url.getURL(), null, creds, cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc = conn.getXMLDocument(RequestMethod.GET, url
					.getURL(), null, creds, cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(
				"Failed to receive expected 404 status code on repeated delete of same record",
				404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull("Contents of deleted record were unexpectedly not null", doc);

	}

	// @Test
	public void testPermissionsPost() throws Exception {
		// TODO check whether this is needed anymore - it should be covered by
		// PostGetDelete above?
		// TODO check whether should be commented back in - Chris was debugging
		// permissions
		// Map<String,Document> parts=new HashMap<String,Document>();
		// ReturnedURL
		// url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
		// assertEquals(201,url.getStatus());
		// int
		// status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		// assertEquals(200,status); // XXX CSPACE-73, should be 404
	}

	/*
	 * @Ignore public void testRolePermissionsPost() throws Exception { //
	 * ReturnedURLurl=conn.getURL(RequestMethod.POST,
	 * "authorization/roles/cbdb4f45-2fac-461b-93ef-6fec21a2ad97/permroles"
	 * ,getDocument("rolepermissions.xml"),creds,cache); //
	 * assertEquals(201,url.getStatus());
	 * 
	 * 
	 * 
	 * // TODO check whether should be commented back in - Chris was debugging
	 * permissions // NOTE this test is more complex than PostGetDelete and
	 * perhaps should remain as a separate test? //create a permission //
	 * Map<String,Document> parts=new HashMap<String,Document>(); // ReturnedURL
	 * url
	 * =conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument
	 * ("permissions.xml"),creds,cache); // assertEquals(201,url.getStatus());
	 * 
	 * //create permissionRole for the permission above // url =
	 * conn.getURL(RequestMethod.POST,
	 * "authorization/permissions/"+url.getURLTail()+"/permroles",
	 * getDocument("rolepermissions.xml"), creds, cache); // assertEquals(201,
	 * url.getStatus()); //delete the permissionRole // int
	 * status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	 * // assertEquals(200,status); // XXX CSPACE-73, should be 404
	 * 
	 * //delete the permission //
	 * status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	 * // assertEquals(200,status); // XXX CSPACE-73, should be 404 }
	 */

	@Test
	public void testAuthorityCreateUpdateDelete() throws Exception {

		Map<String, Document> parts = new HashMap<String, Document>();
		parts.put("personauthorities_common", getDocument("personAuth.xml"));
		Map<String, Document> parts1 = new HashMap<String, Document>();
		parts1.put("personauthorities_common", getDocument("personAuth.xml"));
		String id;
		// CREATE
		ReturnedURL url = conn.getMultipartURL(RequestMethod.POST,
				"personauthorities/", parts, creds, cache);
		assertEquals(201, url.getStatus());
		id = url.getURLTail();
		// UPDATE
		ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(
				RequestMethod.PUT, "personauthorities/" + id, parts1, creds,
				cache);
		assertEquals(200, doc.getStatus()); // XXX shouldn't this be 201?
		// DELETE
		conn.getNone(RequestMethod.DELETE, "personauthorities/" + id, null,
				creds, cache);
		assertEquals(200, doc.getStatus());

	}

	@Test
	public void testObjectsPut() throws Exception {
		Map<String, Document> parts = new HashMap<String, Document>();
		parts.put("collectionobjects_common", getDocument("obj1.xml"));
		ReturnedURL url = conn.getMultipartURL(RequestMethod.POST,
				"collectionobjects/", parts, creds, cache);
		assertEquals(201, url.getStatus());
		ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(
				RequestMethod.PUT, url.getURL(), buildObject("32", "obj2.xml",
						"collectionobjects_common"), creds, cache);
		assertEquals(201, url.getStatus()); // 201?
		doc = conn.getMultipartXMLDocument(RequestMethod.GET, url.getURL(),
				null, creds, cache);
		assertEquals(200, doc.getStatus());
		String num = doc.getDocument("collectionobjects_common")
				.selectSingleNode("collectionobjects_common/objectNumber")
				.getText();
		assertEquals("32", num);
	}

	// TODO pre-emptive cache population

	private Map<String, Document> buildObject(String objid, String src,
			String part) throws DocumentException, IOException {
		InputStream data_stream = getResource(src);
		String data = IOUtils.toString(data_stream);
		data_stream.close();
		data = data.replaceAll("<<objnum>>", objid);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(new StringReader(data));
		Map<String, Document> parts = new HashMap<String, Document>();
		parts.put(part, doc);
		return parts;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSearch() throws Exception {
		// Insert one non-aardvark
		Map<String, Document> parts = new HashMap<String, Document>();
		Document doc1 = getDocument("obj1.xml");
		parts.put("collectionobjects_common", doc1);
		ReturnedURL url = conn.getMultipartURL(RequestMethod.POST,
				"collectionobjects/", parts, creds, cache);
		assertEquals(201, url.getStatus());
		String uid1 = url.getURL();
		String non = url.getURLTail();
		// Insert one aardvark
		parts = new HashMap<String, Document>();
		Document doc2 = getDocument("obj-search.xml");
		parts.put("collectionobjects_common", doc2);
		url = conn.getMultipartURL(RequestMethod.POST, "collectionobjects/",
				parts, creds, cache);
		String uid2 = url.getURL();
		assertEquals(201, url.getStatus());
		String good = url.getURLTail();
		// search for aardvark
		ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,
				"collectionobjects?kw=aardvark", null, creds, cache);
		assertEquals(200, doc.getStatus());
		Set<String> csids = new HashSet<String>();
		for (Node n : (List<Node>) doc
				.getDocument()
				.selectNodes(
						"abstract-common-list/list-item/csid")) {
			csids.add(n.getText());
		}

		// delete non-aadvark and aadvark
		int status = conn.getNone(RequestMethod.DELETE, uid1, null, creds,
				cache);
		assertEquals(200, status); // XXX CSPACE-73, should be 404
		status = conn.getNone(RequestMethod.DELETE, uid2, null, creds, cache);
		assertEquals(200, status); // XXX CSPACE-73, should be 404

		// test
		assertFalse(csids.size() == 0);
		assertTrue(csids.contains(good));
		assertFalse(csids.contains(non));

	}
}
