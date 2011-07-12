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
	
	
	/*	
	private final static String loanoutCreate = "{\"loanPurpose\":\"research\",\"loanedObjectStatus\":\"agreed\",\"loanOutNumber\":\"LO2010.1.3\",\"loanOutNote\":\"loan out notes\",\"specialConditionsOfLoan\":\"loanout conditions\",\"lendersAuthorizationDate\":\"May 27, 2010\",\"loanedObjectStatusDate\":\"May 28, 2010\",\"loanReturnDate\":\"May 26, 2010\",\"loanOutDate\":\"May 25, 2010\",\"loanRenewalApplicationDate\":\"May 24, 2010\",\"loanedObjectStatusNote\":\"status note\"}";
	private final static String loaninCreate = "{\"loanInNumber\":\"LI2010.1.2\",\"lendersAuthorizer\":\"lendersAuthorizer\",\"lendersAuthorizationDate\":\"lendersAuthorizationDate\",\"lendersContact\":\"lendersContact\",\"loanInContact\":\"loanInContact\",\"loanInConditions\":\"loanInConditions\",\"loanInDate\":\"loanInDate\",\"loanReturnDate\":\"loanReturnDate\",\"loanRenewalApplicationDate\":\"loanRenewalApplicationDate\",\"loanInNote\":\"loanInNote\",\"loanPurpose\":\"loanPurpose\"}";
	private final static String intakeCreate = "{\"normalLocation\": \"normalLocationX\",\"fieldCollectionEventName\": \"fieldCollectionEventNameX\",\"earliestDateCertainty\": \"earliestDateCertaintyX\",\"earliestDate\": \"earliestDateX\",\"latestDate\": \"latestDateX\",\"entryNumber\": \"entryNumberX\",\"insurancePolicyNumber\": \"insurancePolicyNumberX\",\"depositorsRequirements\": \"depositorsRequirementsX\",\"entryReason\": \"entryReasonX\",\"earliestDateQualifier\": \"earliestDateQualifierX\"}";
	private final static String objectCreate = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"2\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartments\":[{\"responsibleDepartment\":\"new DEPT\"}],\"objectNameGroup\": [{ \"objectName\": \"new OBJNAME\",}]}";
	//private final static String objectCreate = "{\"accessionNumber\": \"new OBJNUM\", \"description\": \"new DESCRIPTION\", \"descInscriptionInscriber\": \"new INSCRIBER\", \"objectNumber\": \"1\", \"objectTitle\": \"new TITLE\", \"comments\": \"new COMMENTS\", \"distinguishingFeatures\": \"new DISTFEATRES\", \"responsibleDepartment\": \"new DEPT\",\"briefDescriptions\": [ { \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WAAAA\", \"primary\": \"arg\" }, { \"briefDescription\": \"WOOOOP\", \"primary\": \"bob\" } ], \"objectName\": \"new OBJNAME\"}";
	private final static String movementCreate = "{\"normalLocation\":\"blah\",\"movementContact\":\"blah\",\"movementReferenceNumber\":\"MV2010.99\",\"currentLocationFitness\":\"blah\",\"removalDate\":\"2012-04-29\",\"locationDate\":\"2012-01-29\",\"plannedRemovalDate\":\"2012-03-29\",\"movementMethods\":[{\"movementMethod\":\"blah\"}],\"movementNote\":\"blah\",\"reasonForMove\":\"blah\",\"currentLocation\":\"blah\",\"currentLocationNote\":\"blah\"}";
	private final static String acquisitionCreate = "{\"acquisitionReason\":\"acquisitionReason\",\"acquisitionReferenceNumber\":\"acquisitionReferenceNumber\",\"acquisitionMethod\":\"acquisitionMethod\",\"owners\":[{\"owner\":\"urn:cspace:org.collectionspace.demo:orgauthority:id(4bf0090c-7d67-4d92-9370):organization:id(b09db2c1-a849-43b5-8ad1)'Bing+Crosby+Ice+Cream+Sales%2C+Inc.'\"}],\"acquisitionSources\":[{\"acquisitionSource\": \"11111\"},{\"acquisitionSource\": \"22222\"}]}";
	private final static String roleCreate = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_1_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	private final static String role2Create = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_2_TEST_" + d.toString() + "\", \"description\": \"this role is also for test users\"}";
	private final static String personCreate = "{\"fields\":{\"displayName\":\"TEST_PERSON4_display\"}}";


	private final static String permissionDelete = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]}, {\"action\": [{ \"name\": \"DELETE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionRead = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"READ\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionWrite = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionNone = "{ \"resourceName\": \"intake\", \"actions\": [], \"effect\": \"PERMIT\" }";
	private final static String permission2Write = "{ \"resourceName\": \"loanin\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permission2None = "{ \"resourceName\": \"loanin\", \"actions\": [], \"effect\": \"PERMIT\" }";
	
	private final static String permroleCreate = "{ \"permissions\": [ {\"recordType\": \"Object Cataloging\", \"permission\": \"write\"}, {\"recordType\": \"Intake\", \"permission\": \"write\"}, {\"recordType\": \"Acquisition\", \"permission\": \"write\"}, {\"recordType\": \"Loan In\", \"permission\": \"read\"}, {\"recordType\": \"Loan out\", \"permission\": \"read\"}] }";

	private final static String accountroleCreate = "{ \"account\": { \"userId\": \"\", \"screenName\": \"\", \"accountId\": \"\" },  \"role\": [{ \"roleName\": \"\", \"roleId\": \"\" }] }";

	//private final static String testStr3 = "{\"a\":\"b\",\"id\":\"***misc***\",\"objects\":\"***objects***\",\"intake\":\"***intake***\"}";
	//private final static String testStr4 = "{\"a\":\"b\",\"id\":\"MISC2009.1\",\"objects\":\"OBJ2009.1\",\"intake\":\"IN2009.1\"}";
	//private final static String testStr5 = "{\"a\":\"b\",\"id\":\"MISC2009.2\",\"objects\":\"OBJ2009.2\",\"intake\":\"IN2009.2\"}";

	private final static String user2Create = "{\"userId\": \"unittest2@collectionspace.org\",\"screenName\": \"unittestzzz\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
	private final static String user2Update = "{\"userId\": \"unittest2@collectionspace.org\",\"screenName\": \"unittestzzz\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"active\"}";
	private final static String user2Email = "{\"email\": \"unittest2@collectionspace.org\", \"debug\" : true }";
	//private final static String userEmail = "{\"email\": \"unittest@collectionspace.org\", \"debug\" : true }";
	private final static String user88Create = "{\"userId\": \"unittest88@collectionspace.org"+ d.toString() +"\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
	*/
	private FileStorage store;
	private UserDetailsReset udreset;

	private String cookie;
	
	private static String tmp=null;

	private static synchronized String tmpdir() {
		if(tmp==null) {
			tmp=System.getProperty("java.io.tmpdir");
		}
		return tmp;
	}

	private void rm_r(File in) {
		for(File f : in.listFiles()) {
			if(f.isDirectory())
				rm_r(f);
			f.delete();
		}
	}
	/** 
	 * Creates a file storage area (store)
	 * 
	 * @throws IOException
	 * @throws CSPDependencyException
	 */
	@Before public void setup() throws IOException, CSPDependencyException {
		File tmp=new File(tmpdir());
		File dir=new File(tmp,"ju-cspace");
		if(dir.exists())
			rm_r(dir);
		if(!dir.exists())
			dir.mkdir();
		store=new FileStorage(dir.toString());
	}

	/**
	 * Utilities to deal with JSON
	 **
	 * Write JSON to store
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnderlyingStorageException
	 * @throws UnimplementedException
	 */
	@Test public void writeJSONToFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.autocreateJSON("/cataloging/", jsonObject);
	}
	/**
	 * Read JSON from store
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnderlyingStorageException
	 * @throws UnimplementedException
	 */
	@Test public void readJSONFromFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		JSONObject restrictions = new JSONObject();
		String path=store.autocreateJSON("/cataloging/", jsonObject);
		JSONObject resultObj = store.retrieveJSON("/cataloging/"+path,restrictions);
		JSONObject testObj = new JSONObject(testStr);
		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
	}

	/**
	 * Handles non-existing JSON
	 * @throws JSONException
	 * @throws UnderlyingStorageException
	 * @throws UnimplementedException
	 */
	@Test public void testJSONNotExist() throws JSONException, UnderlyingStorageException, UnimplementedException {
		try
		{
			JSONObject restrictions = new JSONObject();
			store.retrieveJSON("nonesuch.json",restrictions);
			assertTrue(false);
		}
		catch (ExistException onfe) {}
	}
	/**
	 * Update stored JSON
	 * @throws ExistException
	 * @throws JSONException
	 * @throws UnderlyingStorageException
	 * @throws UnimplementedException
	 */
	@Test public void testJSONUpdate() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject restrictions = new JSONObject();
		JSONObject jsonObject = new JSONObject(testStr2);
		String id1=store.autocreateJSON("/cataloging/", jsonObject);
		jsonObject = new JSONObject(testStr);
		store.updateJSON("/cataloging/"+id1, jsonObject);		
		JSONObject resultObj = store.retrieveJSON("/cataloging/"+id1,restrictions);
		JSONObject testObj = new JSONObject(testStr);
		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
	}
	/**
	 * Handles update to non-exiting JSON
	 * @throws ExistException
	 * @throws JSONException
	 * @throws UnderlyingStorageException
	 * @throws UnimplementedException
	 */
	@Test public void testJSONNoUpdateNonExisting() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		try {
			store.updateJSON("/cataloging/json1.test", jsonObject);
			assertTrue(false);
		} catch(ExistException e) {}
	}

	private File tmpSchemaFile(String type,boolean sj) {
		File sroot=new File(store.getStoreRoot()+"/uispecs");
		if(!sroot.exists())
			sroot.mkdir();
		File schema=new File(store.getStoreRoot()+"/uispecs/"+type);
		if(!schema.exists())
			schema.mkdir();
		return new File(schema,sj?"uispec.json":"test-json-handle.tmp");
	}

	private void createSchemaFile(String type,boolean sj,boolean alt) throws IOException {
		File file=tmpSchemaFile(type,sj);
		FileOutputStream out=new FileOutputStream(file);
		IOUtils.write(alt?testStr2:testStr,out);
		out.close();
	}

	private void deleteSchemaFile(String type,boolean sj) {
		File file=tmpSchemaFile(type,sj);
		file.delete();
	}

	/**
	 * Various Tests on Schema Store
	 * @throws IOException
	 * @throws JSONException
	 */
	@Test public void testSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",false,true);
		JSONObject j=schema.getSchema("collection-object/test-json-handle.tmp");
		JSONUtils.checkJSONEquiv(testStr2,j.toString());
		deleteSchemaFile("collection-object",false);
	}

	@Test public void testDefaultingSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",true,true);
		JSONObject j=schema.getSchema("collection-object");
		JSONUtils.checkJSONEquiv(testStr2,j.toString());
		deleteSchemaFile("collection-object",true);
	}

	@Test public void testTrailingSlashOkayOnSchema() throws Exception {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",true,true);
		JSONObject j=schema.getSchema("collection-object/");
		JSONUtils.checkJSONEquiv(testStr2,j.toString());
		deleteSchemaFile("collection-object",true);	
	}
/**
 * Start up Jetty
 * @throws Exception
 */
	@Test public void testJettyStartupWorks() throws Exception {
		setupJetty();
	}


	
/**
 * Test the User Profiles 
 * 
 * A password reset is a 2 stage process:
 * The user asks to reset the password - POST with users email address ( App generates a token)
 * The user sets a new pw - POST ( App layer checks pw, email and token)
 * 
 * I'm unsure as to what was intended here.  Seems to send a password reset with the pw unchanged.
 * Checks User Id is unchanged then does update and again checks User Id unchanged
 * I'm confused about what was intended by the CheckJSONEquiv... routines as with the current data they would
 * never match. 
 * @throws Exception
 */
	@Test public void testUserProfilesWithReset() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out;
		//delete user if already exists 
		out = createUser(jetty,user2Create);
		String id=out.getHeader("Location");

		out = GETData(id,jetty);
		log.info(out.getContent());
		//ask to reset
		out = POSTData("/passwordreset/",user2Email,jetty);
				
		//this should fail - switch this on when we want to test with a failing token
		/*JSONObject obj = new JSONObject(out.getContent());
		Long token = Long.parseLong(obj.getString("token"));
		token -= (8*24*60*60*10000);
		obj.put("token", token);
		*/
		JSONObject obj = new JSONObject(out.getContent());
		obj.put("password", "testetst");

		// Reset password
		out = POSTData("/resetpassword/",obj,jetty);
		
		// Read - seems to be failing -need to refresh jetty - probably because I hashed the login to get teh reset
		jetty=setupJetty();
		HttpTester out2 = GETData(id,jetty);
		
		// Checks User Id is unchanged
		log.info(out2.getContent());
		JSONObject user2AfterReset=new JSONObject(out2.getContent());
		JSONObject user2CreateCopy=new JSONObject(user2Create);
		assertEquals(user2AfterReset.getJSONObject("fields").get("userId").toString(),user2CreateCopy.get("userId").toString());
		
		// Don't know what this is aiming at (commented out already) but the much of the content is different eg status
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2CreateCopy));
		
		// Updates - changes screen name and status
		out = PUTData(id,makeSimpleRequest(user2Update),jetty);
		
		// Read
		out = GETData(id,jetty);
		
		// Check User Id is unchanged
		JSONObject user2AfterUpdate=new JSONObject(out.getContent());
		JSONObject user2UpdateCopy=new JSONObject(user2Update);
		assertEquals(user2AfterUpdate.getJSONObject("fields").get("userId").toString(),user2UpdateCopy.get("userId").toString());
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2UpdateCopy));
		
		// Delete
		DELETEData(id,jetty);
		
		
	}
	

	/**
	 * Tests CRUD for different Store Types
	 * @throws Exception
	 */
	@Test public void testMultipleStoreTypes() throws Exception {
		ServletTester jetty=setupJetty();
		testPostGetDelete(jetty, "/cataloging/", objectCreate, "distinguishingFeatures");
		testPostGetDelete(jetty, "/media/", mediaCreate, "identificationNumber");
		testPostGetDelete(jetty, "/movement/", movementCreate, "movementReferenceNumber");
		testPostGetDelete(jetty, "/intake/", intakeCreate, "entryReason");
		testPostGetDelete(jetty, "/loanout/", loanoutCreate, "loanOutNote");
		testPostGetDelete(jetty, "/loanin/", loaninCreate, "loanInNote");
		testPostGetDelete(jetty, "/acquisition/", acquisitionCreate, "acquisitionReason");
		testPostGetDelete(jetty, "/role/", roleCreate, "description");
		//testPostGetDelete(jetty, "/permission/", permissionRead, "resourceName");
		//testPostGetDelete(jetty, "/permrole/", permroleCreate, "");
	}
	/**
	 * Tests a static page
	 * @throws Exception
	 */
	//@Test 
	public void testServeStatic() throws Exception {

		HttpTester out = GETData("/chain.properties",setupJetty());
		assertTrue(out.getContent().contains("cspace.chain.store.dir"));
	}


	
	@Test public void test2() throws Exception{
		
		JSONObject user = new JSONObject();
			user.put("userid", "tester@cs.org");
			user.put("password", "testtest");
		
		ServletTester jetty=setupJetty();
		//ServletTester jetty=setupJetty(false,"tenant2.xml");
		
		String csid = "/loanin/search?query=LI2011.1.2%20-%20Justine%20Johnstone&pageSize=10";
		//http://nightly.collectionspace.org/collectionspace/chain/vocabularies/location/source-vocab/relatedTerm
		String test = "{\"relations\":{},\"fields\":{\"relatedTerms\":[{\"_primary\":true}],\"nationalities\":[{\"_primary\":true}],\"schoolsOrStyles\":[{\"_primary\":true}],\"title\":\"\",\"termStatus\":\"provisional\",\"gender\":\"\",\"narrowerContexts\":[{\"_primary\":true}],\"groups\":[{\"_primary\":true}],\"occupations\":[{\"_primary\":true}],\"addressType\":\"\",\"salutation\":\"dear\",\"equivalentContexts\":[{}],\"displayName\":\"bob2\"},\"namespace\":\"persontest1\",\"csid\":\"\"}"; 
		
		HttpTester out;
		out = GETData(csid, jetty);
		
//		DELETEData("/users/c990a163-ef62-4f06-95ae-c362d3fac9ff",jetty);
		//JSONObject fields=new JSONObject(out.getContent()).getJSONObject("fields");
		 //cspace-services/personauthorities/5c642112-f75a-43b4-aff8/items/0e07e795-fb9d-4f39-a848/
		//HttpTester out2=jettyDo(jetty,"POST","/chain/upload",null);
		log.info(out.getContent());
		
	}
	

	/** 
	 * Test List functionality for different Store Types
	 * @throws Exception
	 */
	@Test public void testObjectList() throws Exception {
		ServletTester jetty=setupJetty();

		testLists(jetty, "cataloging", objectCreate, "items");
		testLists(jetty, "intake", intakeCreate, "items");
		testLists(jetty, "loanin", loaninCreate, "items");
		testLists(jetty, "loanout", loanoutCreate, "items");
		testLists(jetty, "acquisition", acquisitionCreate, "items");
		testLists(jetty, "role", roleCreate, "items");
		testLists(jetty, "movement", movementCreate, "items");
		testLists(jetty, "media", mediaCreate, "items");
		//testLists(jetty, "permission", permissionWrite, "items");
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
	 * Tests JSON is created in expected directories
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	@Test public void testDirectories() throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject jsonObject = new JSONObject(testStr);
		String id1=store.autocreateJSON("/a", jsonObject);
		String id2=store.autocreateJSON("/b", jsonObject);
		// Creates dirs a and b as subdir of <Storeroot>/data/
		File d1=new File(store.getStoreRoot());
		assertTrue(d1.exists());
		File d2=new File(d1,"data");
		assertTrue(d2.exists());
		File a=new File(d2,"a");
		assertTrue(a.exists());
		File b=new File(d2,"b");
		assertTrue(b.exists());
		// Checks JSON was stored in these directories
		assertTrue(new File(a,id1+".json").exists());
		assertTrue(new File(b,id2+".json").exists());
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

	@Test public void testUpload() throws Exception {
		ServletTester jetty = setupJetty();
		String filename = getClass().getPackage().getName().replaceAll("\\.","/")+"/darwin-beard-hat.jpg";
		byte[] data = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
		UTF8SafeHttpTester out=POSTBinaryData("/uploads",data,jetty);
		log.info(out.getContent());
		JSONObject response = new JSONObject(out.getContent());
		assertTrue(response.getString("file").contains("/blobs/"));
		assertTrue(response.optString("csid")!=null);
		assertNotSame("",response.optString("csid"));
		// Actual resource
		String read_url = response.getString("file").replaceAll("^.*?/blobs/","/download/")+"/Original";
		UTF8SafeHttpTester out2=GETBinaryData(read_url,jetty,200);
		assertEquals("image/jpeg",out2.getHeader("Content-Type"));
		byte[] img = out2.getBinaryContent();
		assertArrayEquals(img,data);
		System.err.println(response.getString("file"));
	}
	
	@Test public void testMediaWithBlob() throws Exception {
		ServletTester jetty = setupJetty();
		// Create a blob
		String filename = getClass().getPackage().getName().replaceAll("\\.","/")+"/darwin-beard-hat.jpg";
		byte[] data = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
		UTF8SafeHttpTester out2=POSTBinaryData("/uploads",data,jetty);
		log.info(out2.getContent());
		JSONObject response = new JSONObject(out2.getContent());
		System.err.println(response);
		String blob_id = response.getString("csid");
		// Create
		JSONObject media=new JSONObject(mediaCreate);
		JSONObject blob = new JSONObject();
		blob.put("updatedAt","2011-05-06T18:23:27Z");
		blob.put("createdAt","2011-05-06T18:23:27Z");
		blob.put("name","darwin-beard-hat.jpg");
		blob.put("mimeType","image/jpeg");
		blob.put("length",data.length);
		JSONArray blobs = new JSONArray();
		blobs.put(blob);
		media.put("blobs",blobs);
		media.put("blobCsid",blob_id);
		HttpTester out = POSTData("/media",makeRequest(media),jetty);
		assertEquals(201,out.getStatus());
		String id=out.getHeader("Location");
		// Get
		out = GETData(id,jetty);
		JSONObject content=new JSONObject(out.getContent());
		log.info(out.getContent());
		// Check the hairy image URLs are present
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgOrig").endsWith("/download/"+blob_id+"/Original"));
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgThumb").endsWith("/download/"+blob_id+"/Thumbnail"));
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgMedium").endsWith("/download/"+blob_id+"/Medium"));
		// Get derivatives
		String read_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgOrig");
		String read2_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgThumb");
		String read3_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgMedium");
		UTF8SafeHttpTester out3=GETBinaryData(read_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));
		byte[] img = out3.getBinaryContent();
		assertArrayEquals(img,data);
		out3=GETBinaryData(read2_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));
		out3=GETBinaryData(read3_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));

		
		// Delete
		DELETEData(id,jetty);
	}
	
	@Test public void testMediaUISpec() throws Exception {
		ServletTester jetty = setupJetty();
		HttpTester out = GETData("/media/uispec",jetty);
		assertEquals(200,out.getStatus());
		System.err.println(out.getContent());
		JSONObject spec=new JSONObject(out.getContent());
		assertEquals("${fields.blobs.0.length}",spec.getJSONObject("recordEditor").getString(".csc-blobs-length"));
	}
	
	@Test public void testRolesPermsUI() throws Exception {

		ServletTester jetty = setupJetty();
//		create role with permissions
		JSONObject rolepermsdata = createRoleWithPermission(roleCreate,"loanin", "loanout"); 
		JSONObject roleperms2data = createRoleWithPermission(roleCreate,"acquisition", "intake"); 

		log.info(rolepermsdata.toString());
		HttpTester out = POSTData("/role/",makeRequest(rolepermsdata),jetty);
		String role_id = out.getHeader("Location");

		//get role
		out = GETData(role_id,jetty);

		//test
		JSONObject data = new JSONObject(out.getContent());
		log.info(data.toString());
		

		//update role
		log.info("roleperms2data:"+roleperms2data.toString());
		out = PUTData(role_id,makeRequest(roleperms2data),jetty);
		//test
		JSONObject dataUP = new JSONObject(out.getContent());
		
		
		
		//delete role		
		DELETEData(role_id,jetty);
		
		
		//test data GET
		log.info("GET: "+data.toString());
		JSONArray perms = data.getJSONObject("fields").getJSONArray("permissions");
		int test = 0;
		for(int i=0; i<perms.length();i++){
			JSONObject thisperm = perms.getJSONObject(i);
			if(thisperm.getString("resourceName").equals("loanout")){
				assertEquals("write",thisperm.getString("permission"));
				test++;
			}
			if(thisperm.getString("resourceName").equals("loanin")){
				assertEquals("read",thisperm.getString("permission"));
				test++;
			}
		}
		assertEquals("failed to find loansout and loansin",2,test);


		//test data UPDATE
		log.info("UPDATA: "+dataUP.toString());
		JSONArray permsUP = dataUP.getJSONObject("fields").getJSONArray("permissions");
		int testUP = 0;
		for(int i=0; i<permsUP.length();i++){
			JSONObject thisperm = permsUP.getJSONObject(i);
			if(thisperm.getString("resourceName").equals("intake")){
				assertEquals("write",thisperm.getString("permission"));
				testUP++;
			}
			if(thisperm.getString("resourceName").equals("acquisition")){
				assertEquals("read",thisperm.getString("permission"));
				testUP++;
			}
		}
		assertEquals("failed to find acquisitions and intakes",2,testUP);
	}
	
	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty();
		JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
		JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		HttpTester out = POSTData("/users/",makeRequest(userdata),jetty);
		String userid = out.getHeader("Location");
		log.info("2::"+userid);

		out = GETData(userid,jetty);

		String screenname = userdata2.getString("userName");
		userdata2.remove("userName");
		userdata2.put("screenName", screenname);
		
		
		out = PUTData(userid,makeRequest(userdata2),jetty);

		out = GETData(userid,jetty);

		JSONObject data = new JSONObject(out.getContent());
		JSONArray roles = data.getJSONObject("fields").getJSONArray("role");
		//delete roles

		//Delete the roles
		String roles_id1 = "/role/" + userdata.getJSONArray("role").getJSONObject(0).getString("roleId");
		String roles_id2 = "/role/" + userdata2.getJSONArray("role").getJSONObject(0).getString("roleId");


		DELETEData(roles_id1,jetty);
		DELETEData(roles_id2,jetty);
		
		//delete user
		DELETEData(userid,jetty);
		

		//test role_1 deleted to payload
		assertEquals("Should only be one role, if more then it didn't delete, if less then it didn't add",1,roles.length());

		//test role_2 added to payload
		for(int i=0; i<roles.length();i++){
			JSONObject role = roles.getJSONObject(i);
			//assertEquals()
			if(!role.getString("roleName").equals("ROLE_SPRING_ADMIN")){
				assertEquals(role.getString("roleName"),userdata2.getJSONArray("role").getJSONObject(0).getString("roleName"));
			}
		}
		
	}


}
