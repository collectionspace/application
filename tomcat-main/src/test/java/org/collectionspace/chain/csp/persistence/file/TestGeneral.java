/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
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

import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsReset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provides general tests for the WebAPP that are not covered with the more specific tests such as TestNameThroughWebapp
 * @author 
 *
 */
public class TestGeneral  {
	private static final Logger log=LoggerFactory.getLogger(TestGeneral.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			log.error("TestGeneral: Could not set up jetty! " + ex.getLocalizedMessage());
		}
	}
	
	@BeforeClass public static void testInitialise() throws Exception {
		HttpTester out = tester.GETData(TestBase.AUTHS_INIT_PATH, jetty);
		log.info(out.getContent());
	}

	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}

	
	private final static String testStr2 = "{\"accessionNumber\":\"OBJNUM\",\"description\":\"DESCRIPTION\",\"descInscriptionInscriber\":\"INSCRIBER\",\"objectNumber\":\"1\",\"objectTitle\":\"TITLE\",\"comments\":\"COMMENTS\",\"distinguishingFeatures\":\"DISTFEATURES\",\"responsibleDepartment\":\"DEPT\",\"objectName\":\"OBJNAME\"}";
	private final static Date d = new Date();
	private final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"inscriptionContentInscriber\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
	
	
	//@Test 
	public void test2() throws Exception{
		
		JSONObject user = new JSONObject();
			user.put("userid", "admin@testsci.collectionspace.org");
			user.put("password", "Administrator");

		//ServletTester jetty=setupJetty("testsci", user);
		//ServletTester jetty=setupJetty(false,"tenant2.xml");
	//	http://nightly.collectionspace.org/collectionspace/tenant/core/invokereport/88b3bdb5-a7fd-4e39-aaa1
	//	String csid = "/reporting/search?doctype=Acquisition";
		//uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
	//	String csid = "/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related";
	//	String csid = "/cataloging/termList/dateEarliestSingleEra";
//		String csid = "/role/08a84ce3-9236-468b-b8c2-66d07706b273";
		String csid = TestBase.MAIN_PERSON_INSTANCE_PATH;
		//http://nightly.collectionspace.org/collectionspace/chain/vocabularies/location/source-vocab/relatedTerm
//		String test = "{\"fields\":{\"lenderGroup\":[{\"_primary\":true}],\"loanPurposes\":[{\"loanPurpose\":\"\",\"_primary\":true}],\"updatedBys\":[{\"_primary\":true}],\"loanInNumbers\":[{\"_primary\":true}],\"updatedAtStart\":\"2011-09-05\"},\"operation\":\"or\"}";
		String test = "{\"termsUsed\":[],\"relations\":[],\"csid\":\"\",\"refobjs\":[],\"namespace\":\"person\"," +
				"\"fields\":{\"surName\":\"\",\"birthPlace\":\"\",\"deathDate\":\"\",\"updatedBy\":\"admin@core.collectionspace.org\"," +
				"\"nationalities\":[{\"_primary\":true,\"nationality\":\"\"}],\"shortIdentifier\":\"bobclampett\"," +
				"\"schoolsOrStyles\":[{\"_primary\":true,\"schoolOrStyle\":\"\"}],\"title\":\"\",\"initials\":\"\"," +
				"\"createdAt\":\"2011-10-10T13:46:44Z\",\"nameNote\":\"save something\",\"termStatus\":\"\",\"gender\":\"\",\"birthDate\":\"\",\"foreName\":\"\"," +
				"\"groups\":[{\"_primary\":true,\"group\":\"\"}],\"recordtype\":\"person\",\"nameAdditions\":\"\"," +
				"\"occupations\":[{\"occupation\":\"\",\"_primary\":true}],\"middleName\":\"\",\"bioNote\":\"\"," +
				"\"csid\":\"79a356bd-17ab-47cf-8174\",\"deathPlace\":\"\",\"updatedAt\":\"2011-10-10T14:20:28Z\"," +
				"\"createdBy\":\"admin@core.collectionspace.org\",\"equivalentContexts\":[],\"authorityid\":\"34d85e61-7d0f-4b37-a6c4\"," +
				"\"displayName\":\"Bob Clampett\",\"salutation\":\"\"," +
				"\"refid\":\"urn:cspace:core.collectionspace.org:personauthorities:name(person):item:name(bobclampett)'Bob Clampett'\"," +
				"\"telephoneNumberGroup\":[{\"_primary\":true,\"telephoneNumberType\":\"\"}],\"emailGroup\":[{\"emailType\":\"\",\"_primary\":true}]," +
				"\"faxNumberGroup\":[{\"_primary\":true,\"faxNumberType\":\"\"}],\"webAddressGroup\":[{\"_primary\":true,\"webAddressType\":\"\"}]," +
				"\"addressGroup\":[{\"_primary\":true,\"addressType\":\"\"}]," +
				"\"narrowerContexts\":[{\"_primary\":true,\"narrowerContext\":\"urn:cspace:core.collectionspace.org:personauthorities:name(person):item:name(tommix)'Tom Mix'\"}]," +
				"\"relatedTerms\":[{\"_primary\":true}]," +
				"\"broaderContext\":\"urn:cspace:core.collectionspace.org:personauthorities:name(person):item:name(georgebancroft)'George Bancroft'\"}}";
		HttpTester out;
		//loanedObjectStatus

		//out = jettyDo(jetty, "GET", "/tenant/testsci/authorities/vocab/initialize", null);

		//out = jettyDo(jetty, "GET", "/tenant/testsci/cataloging/uispec", null);

		//out = GETData(csid,  jetty);
		out = tester.POSTData(csid, test, jetty);
		
//		DELETEData("/users/c990a163-ef62-4f06-95ae-c362d3fac9ff",jetty);
		//JSONObject fields=new JSONObject(out.getContent()).getJSONObject("fields");
		 //cspace-services/personauthorities/5c642112-f75a-43b4-aff8/items/0e07e795-fb9d-4f39-a848/
		//HttpTester out2=jettyDo(jetty,"POST","/chain/upload",null);
		//
	log.info(out.getContent());
		
	}
	


	/* XXX I don't think this is tetsing what it needs to */
	/**
	 * Writes a series of Objects then does a Read using a trailing slash
	 * and checks each object is found
	 */
	@Test public void testTrailingSlashOkayOnList() throws Exception {
		
		// clear (avoid paging)
		JSONArray items=null;

		
		HttpTester out1 = tester.POSTData("/cataloging",tester.makeSimpleRequest(testStr2),jetty);
		HttpTester out2 = tester.POSTData("/cataloging",tester.makeSimpleRequest(testStr2),jetty);
		HttpTester out3 = tester.POSTData("/cataloging",tester.makeSimpleRequest(testStr2),jetty);
		// Read with a trailing slash
		HttpTester out = tester.GETData("/cataloging/",jetty);
		
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
			out = tester.GETData("/" + objtype + "/?pageNum=" + pgNum + "&pageSize=" + pgSz, jetty);
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
		tester.DELETEData(out1.getHeader("Location"),jetty);
		tester.DELETEData(out2.getHeader("Location"),jetty);
		tester.DELETEData(out3.getHeader("Location"),jetty);

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
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());  //REM - Replace.  This is pre-JDK 1.4 code, from the days when JSSE was a separate download.  Fix the imports so they refer to the classes in javax.net.ssl If you really want to get hold of a specific instance, you can use Security.getProvider(name). You'll find the appropriate names in the providers documentation. 
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
		
		//create person authority to use
		String personStr = "{\"shortIdentifier\":\"mytestperson\",\"personTermGroup\":[{\"termDisplayName\":\"TEST my test person\"}]}";

		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",tester.makeSimpleRequest(personStr),jetty);
		String person_url=out.getHeader("Location");
		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("fieldCollectionPlace",urn);
		
		//create object
		out = tester.POSTData("/cataloging/",testdata,jetty);
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		//read and check
		out = tester.GETData(id,jetty);
		JSONObject one = new JSONObject(tester.getFields(out.getContent()));
		log.info(one.toString());
		assertEquals(one.get("fieldCollectionPlace"), urn);
		//assertEquals(one.get("de-urned-fieldCollectionPlace"), "TEST my test person");

		//clean up
		tester.DELETEData(id,jetty);

		tester.DELETEData("/vocabularies/"+person_url,jetty);
		
	}
	/**
	 * Checks a vocabulary (here a person) can be linked to an object 
	 * @throws Exception
	 */
	@Test public void testTermsUsedVocab() throws Exception {
		//create person authority to use
		String personStr = "{\"personTermGroup\":[{\"termDisplayName\":\"TEST my test person\"}]}";
		HttpTester out = tester.POSTData(TestBase.MAIN_PERSON_INSTANCE_PATH+"/",tester.makeSimpleRequest(personStr),jetty);
		String person_url=out.getHeader("Location");
		

		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("contentPeople",urn);
		
		//create object
		out = tester.POSTData("/cataloging/",testdata,jetty);
		String id=out.getHeader("Location");
		
		out = tester.GETData(id,jetty);
		
		// I believe the items below where copied from test above and are not needed
		//JSONObject one = new JSONObject(getFields(out.getContent()));
		
		//assertEquals(one.get("contentPeople"), urn);
		//assertEquals(one.get("de-urned-contentPeople"), "TEST my test person2");

		//get the cataloging linked to the vocab item

		out = tester.GETData("/vocabularies"+person_url,jetty);
		
		//clean up
		tester.DELETEData(id,jetty);
		tester.DELETEData("/vocabularies/"+person_url,jetty);
	}
	
	//@Test - no guarentee that the service layer created this report and put it where I could find it
	public void testReports() throws Exception {
		String uipath = "/acquisition/";
		String data = tester.acquisitionCreate();
		HttpTester out;
		// Create
		out = tester.POSTData(uipath, tester.makeSimpleRequest(data),jetty);
		String id = out.getHeader("Location");
		// Retrieve
		out = tester.jettyDo(jetty, "GET", "/tenant/core" + id, null);
		
		HttpTester out3 = tester.jettyDo(jetty, "GET", "/tenant/core/reporting/search/acquisition", null);
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

		HttpTester out2 = tester.POSTData(url,fields.toString(),jetty,"GET");
		log.info(out2.getContent());
		assertEquals(200,out2.getStatus());
		log.info(Integer.toString(out2.getStatus()));
		assertEquals("application/pdf",out2.getHeader("Content-Type"));
		log.info(out2.getHeader("Content-Type"));
		
		// Delete
		tester.DELETEData(id, jetty);

		
	}
	
	
	
	

}
