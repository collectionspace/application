/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import java.security.Security;
 
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.TenantServlet;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsReset;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provides general tests for the WebAPP that are not covered with the more specific tests such as TestNameThroughWebapp
 * @author 
 *
 */
public class TestGeneral extends TestBase {

	private static final Logger log=LoggerFactory.getLogger(TestGeneral.class);
	
	// Set up test data strings 
	private final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
	"\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
	"{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
	"\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
	"\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
	"\"param\":\"type\"}]}";

	private final static String testStr2 = "{\"accessionNumber\":\"OBJNUM\",\"description\":\"DESCRIPTION\",\"descInscriptionInscriber\":\"INSCRIBER\",\"objectNumber\":\"1\",\"objectTitle\":\"TITLE\",\"comments\":\"COMMENTS\",\"distinguishingFeatures\":\"DISTFEATURES\",\"responsibleDepartment\":\"DEPT\",\"objectName\":\"OBJNAME\"}";
	private final static String testStr2a = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"1\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartment\":\"new DEPT\",\"objectName\":\"new OBJNAME\"}";
	private final static Date d = new Date();
	private final static String testStr10 = "{\"roleName\": \"ROLE_USERS_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	private final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"inscriptionContentInscriber\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
	
	
	private UserDetailsReset udreset;





	

	
	@Test public void test2() throws Exception{
		
		JSONObject user = new JSONObject();
			user.put("userid", "tester@cs.org");
			user.put("password", "testtest");
		
		ServletTester jetty=setupJetty();
		//ServletTester jetty=setupJetty(false,"tenant2.xml");
	//	http://nightly.collectionspace.org/collectionspace/tenant/core/invokereport/88b3bdb5-a7fd-4e39-aaa1
	//	String csid = "/reporting/search?doctype=Acquisition";
		//uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		String csid = "/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related";
		//String csid = "/reporting";
		//String csid = "/vocab/languages";
		//http://nightly.collectionspace.org/collectionspace/chain/vocabularies/location/source-vocab/relatedTerm
		String test = "{\"csid\":\"0a63110d-0383-41bb-bdcd\",\"fields\":{\"shortIdentifier\":\"dateera\",\"usedBys\":[{\"usedBy\":\"structureddate:dateLatestEra\"},{\"usedBy\":\"structureddate:dateEarliestSingleEra\"}],\"source\":\"\",\"description\":\"\",\"terms\":[{\"shortIdentifier\":\"bced\",\"csid\":\"4115010a-8a5b-4c73-b9de\",\"authorityid\":\"0a63110d-0383-41bb-bdcd\",\"displayName\":\"BCED\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(0a63110d-0383-41bb-bdcd):item:id(4115010a-8a5b-4c73-b9de)'BCE'\",\"recordtype\":\"vocab\",\"description\":\"dfg\",\"source\":\"dfg\",\"sourcePage\":\"dfg\"},{\"shortIdentifier\":\"ce\",\"csid\":\"eeb4b1ad-ed81-4d05-ad98\",\"authorityid\":\"0a63110d-0383-41bb-bdcd\",\"displayName\":\"CE\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(0a63110d-0383-41bb-bdcd):item:id(eeb4b1ad-ed81-4d05-ad98)'CE'\",\"recordtype\":\"vocab\"}],\"csid\":\"0a63110d-0383-41bb-bdcd\",\"displayName\":\"Date Era\"}}";// +
			//	"{\"csid\":\"99cf26b7-9f4f-445e-8c66\",\"fields\":{\"shortIdentifier\":\"addresstype\",\"terms\":[{\"shortIdentifier\":\"previous\",\"csid\":\"1970ae48-d9e1-4a7e-92a4\",\"authorityid\":\"99cf26b7-9f4f-445e-8c66\",\"displayName\":\"Previous\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(99cf26b7-9f4f-445e-8c66):item:id(1970ae48-d9e1-4a7e-92a4)'Previous'\",\"recordtype\":\"vocab\",\"termName\":\"wer\",\"termSource\":\"r\",\"termStatus\":\"inactive\"},{\"shortIdentifier\":\"street\",\"csid\":\"4ae1d2a1-095b-4dcc-be98\",\"authorityid\":\"99cf26b7-9f4f-445e-8c66\",\"displayName\":\"Street\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(99cf26b7-9f4f-445e-8c66):item:id(4ae1d2a1-095b-4dcc-be98)'Street'\",\"recordtype\":\"vocab\",\"termName\":\"werwe\",\"termDescription\":\"rrr\",\"termSourcePage\":\"r\"},{\"shortIdentifier\":\"alternative\",\"csid\":\"75103d23-00c4-42ff-baf2\",\"authorityid\":\"99cf26b7-9f4f-445e-8c66\",\"displayName\":\"Alternative\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(99cf26b7-9f4f-445e-8c66):item:id(75103d23-00c4-42ff-baf2)'Alternative'\",\"recordtype\":\"vocab\",\"termName\":\"rrrr\"},{\"shortIdentifier\":\"mailing\",\"csid\":\"84d59f3a-d48f-44df-b2d5\",\"authorityid\":\"99cf26b7-9f4f-445e-8c66\",\"displayName\":\"Mailing\",\"refid\":\"urn:cspace:org.collectionspace.demo:vocabulary:id(99cf26b7-9f4f-445e-8c66):item:id(84d59f3a-d48f-44df-b2d5)'Mailing'\",\"recordtype\":\"vocab\",\"termSource\":\"wer\",\"termDescription\":\"www\",\"termName\":\"wer32\"}],\"csid\":\"99cf26b7-9f4f-445e-8c66\",\"displayName\":\"Contact Address Type\",\"description\":\"dfgdfgdfgfddesc\"}}";
		HttpTester out;
		out = GETData(csid,  jetty);
		
//		DELETEData("/users/c990a163-ef62-4f06-95ae-c362d3fac9ff",jetty);
		//JSONObject fields=new JSONObject(out.getContent()).getJSONObject("fields");
		 //cspace-services/personauthorities/5c642112-f75a-43b4-aff8/items/0e07e795-fb9d-4f39-a848/
		//HttpTester out2=jettyDo(jetty,"POST","/chain/upload",null);
		log.info(out.getContent());
		
	}
	


	/* XXX I don't think this is tetsing what it needs to */
	/**
	 * Writes a series of Objects then does a Read using a trailing slash
	 * and checks each object is found
	 */
	@Test public void testTrailingSlashOkayOnList() throws Exception {
		ServletTester jetty=setupJetty();
		
		// clear (avoid paging)
		JSONArray items=null;

		
		HttpTester out1 = POSTData("/cataloging",makeSimpleRequest(testStr2),jetty);
		HttpTester out2 = POSTData("/cataloging",makeSimpleRequest(testStr2),jetty);
		HttpTester out3 = POSTData("/cataloging",makeSimpleRequest(testStr2),jetty);
		// Read with a trailing slash
		HttpTester out = GETData("/cataloging/",jetty);
		
		// Build up a list of items returned
		JSONObject result=new JSONObject(out.getContent());
		items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();

		int pgSz = 100;
		int pgNum = 0;
		String objtype = "cataloging";
		String itemmarker = "items";
		boolean exists = false;
		boolean end = false;
		// Page through 
		do {
			out = GETData("/" + objtype + "/?pageNum=" + pgNum + "&pageSize=" + pgSz, jetty);
			assertEquals(200, out.getStatus());

			/* create list of files */

			JSONObject result1 = new JSONObject(out.getContent());
			JSONArray items1 = result1.getJSONArray(itemmarker);
			if (items1.length() > 0) {
				for (int i = 0; i < items1.length(); i++) {
					files.add("/" + objtype + "/"
							+ items1.getJSONObject(i).getString("csid"));
				}
			} else {
				end = true;
			}

			exists = files.contains(out1.getHeader("Location")) && files.contains(out2.getHeader("Location")) && files.contains(out3.getHeader("Location"));
			pgNum++;
		} while (!end && !exists);
		/* clean up */
		DELETEData(out1.getHeader("Location"),jetty);
		DELETEData(out2.getHeader("Location"),jetty);
		DELETEData(out3.getHeader("Location"),jetty);

		// Check each object is  in the list
		assertTrue(files.contains(out1.getHeader("Location")));
		assertTrue(files.contains(out2.getHeader("Location")));
		assertTrue(files.contains(out3.getHeader("Location")));
		
	}

	
	/**
	 * Sets up and sends email message providing you have set up the email address to send to
	 */
	@Test public void testEmail(){
		Boolean doIreallyWantToSpam = false; // set to true when you have configured the email addresses
		/* please personalises these emails before sending - I don't want your spam. */
	    String from = "admin@collectionspace.org";
	    String[] recipients = {""};

	    String SMTP_HOST_NAME = "localhost";
	    String SMTP_PORT = "25";
	    String message = "Hi, Test Message Contents";
	    String subject = "A test from collectionspace test suite";
	    String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        boolean debug = true;
        
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST_NAME);
        //props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.auth", "false");
        props.put("mail.debug", "true");
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.put("mail.smtp.socketFactory.fallback", "false");
 
        Session session = Session.getDefaultInstance(props);
 
        session.setDebug(debug);
        if(doIreallyWantToSpam){
 
        Message msg = new MimeMessage(session);
        InternetAddress addressFrom;
		try {
			addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);
 
        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
 
        // Setting the Subject and Content Type
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        if(doIreallyWantToSpam){
        	Transport.send(msg);
        	assertTrue(doIreallyWantToSpam);
        }
		} catch (AddressException e) {
			log.debug(e.getMessage());
			assertTrue(false);
		} catch (MessagingException e) {
			log.debug(e.getMessage());
			assertTrue(false);
		}
		}
		//assertTrue(doIreallyWantToSpam);
	}

	/**
	 * Tests the extracted part of the URN is as expected
	 * @throws Exception
	 */
	@Test public void testDeURNedField() throws Exception {
		
		ServletTester jetty=setupJetty();

		//create person authority to use
		String personStr = "{\"shortIdentifier\":\"mytestperson\",\"displayName\":\"TEST my test person\"}";

		HttpTester out = POSTData("/vocabularies/person/",makeSimpleRequest(personStr),jetty);
		String person_id=out.getHeader("Location");
		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("inscriptionContentInscriber",urn);
		
		//create object
		out = POSTData("/cataloging/",testdata,jetty);
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		//read and check
		out = GETData(id,jetty);
		JSONObject one = new JSONObject(getFields(out.getContent()));
		log.info(one.toString());
		assertEquals(one.get("inscriptionContentInscriber"), urn);
		assertEquals(one.get("de-urned-inscriptionContentInscriber"), "TEST my test person");

		//clean up
		DELETEData(id,jetty);

		DELETEData("/vocabularies/"+person_id,jetty);
		
	}
	/**
	 * Checks a vocabulary (here a person) can be linked to an object 
	 * @throws Exception
	 */
	@Test public void testTermsUsedVocab() throws Exception {
		ServletTester jetty=setupJetty();
		//create person authority to use
		String personStr = "{\"displayName\":\"TEST my test person2\"}";
		HttpTester out = POSTData("/vocabularies/person/",makeSimpleRequest(personStr),jetty);
		String person_id=out.getHeader("Location");
		

		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("contentPeople",urn);
		
		//create object
		out = POSTData("/cataloging/",testdata,jetty);
		String id=out.getHeader("Location");
		
		out = GETData(id,jetty);
		
		// I believe the items below where copied from test above and are not needed
		//JSONObject one = new JSONObject(getFields(out.getContent()));
		
		//assertEquals(one.get("contentPeople"), urn);
		//assertEquals(one.get("de-urned-contentPeople"), "TEST my test person2");

		//get the cataloging linked to the vocab item

		out = GETData("/vocabularies"+person_id,jetty);
		
		//clean up
		DELETEData(id,jetty);
		DELETEData("/vocabularies/"+person_id,jetty);
	}
	
	@Test public void testReports() throws Exception {
		ServletTester jetty = setupJetty();
		String uipath = "/acquisition/";
		String data = acquisitionCreate;
		HttpTester out;
		// Create
		out = POSTData(uipath, makeSimpleRequest(data),jetty);
		String id = out.getHeader("Location");
		// Retrieve
		out = jettyDo(jetty, "GET", "/tenant/core" + id, null);
		
		HttpTester out3 = jettyDo(jetty, "GET", "/tenant/core/reporting/search/acquisition", null);
		log.info(out3.getContent());

		JSONObject one = new JSONObject(out.getContent());
		JSONObject list = new JSONObject(out3.getContent());

		String reportcsid = list.getJSONArray("reportlist").getString(0);
		String path = one.getString("csid");

		JSONObject report = new JSONObject();
		report.put("docType", "acquisition");
		report.put("singleCSID", path);
		report.put("mode", "single");
		JSONObject fields = new JSONObject();
		fields.put("fields", report);
		
		String url = "/invokereport/"+reportcsid;

		HttpTester out2 = POSTData(url,fields.toString(),jetty,"GET");
		log.info(out2.getContent());
		assertEquals(200,out2.getStatus());
		log.info(Integer.toString(out2.getStatus()));
		assertEquals("application/pdf",out2.getHeader("Content-Type"));
		log.info(out2.getHeader("Content-Type"));
		
		// Delete
		DELETEData(id, jetty);

		
	}
	
	
	
	

}
