package org.collectionspace.chain.csp.persistence.file;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.webui.userdetails.UserDetailsReset;
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
public class TestGeneral {

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
	/*private final static String permissionDelete = "{ \"resourceName\": \"resourceName_"+d.toString()+"\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]}, {\"action\": [{ \"name\": \"DELETE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionRead = "{ \"resourceName\": \"resourceName_"+d.toString()+ "\", \"actions\": [ {\"action\": [{ \"name\": \"READ\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionWrite = "{ \"resourceName\": \"resourceName_"+d.toString()+"\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permissionNone = "{ \"resourceName\": \"resourceName_"+d.toString()+"\", \"actions\": [], \"effect\": \"PERMIT\" }";
	private final static String permission2Write = "{ \"resourceName\": \"resourceName2_"+d.toString()+"\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	private final static String permission2None = "{ \"resourceName\": \"resourceName2_"+d.toString()+"\", \"actions\": [], \"effect\": \"PERMIT\" }";
	*/
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
	private final static String testStr10 = "{\"roleName\": \"ROLE_USERS_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	private final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"inscriptionContentInscriber\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
	private final static String user88Create = "{\"userId\": \"unittest88@collectionspace.org"+ d.toString() +"\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
	
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
		store.autocreateJSON("/objects/", jsonObject);
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
		String path=store.autocreateJSON("/objects/", jsonObject);
		JSONObject resultObj = store.retrieveJSON("/objects/"+path);
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
			store.retrieveJSON("nonesuch.json");
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
		JSONObject jsonObject = new JSONObject(testStr2);
		String id1=store.autocreateJSON("/objects/", jsonObject);
		jsonObject = new JSONObject(testStr);
		store.updateJSON("/objects/"+id1, jsonObject);		
		JSONObject resultObj = store.retrieveJSON("/objects/"+id1);
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
			store.updateJSON("/objects/json1.test", jsonObject);
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

	private void login(ServletTester tester) throws IOException, Exception {
		String test = "{\"userid\":\"test@collectionspace.org\",\"password\":\"testtest\"}";
		HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
		log.info(out.getContent());
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.debug("Got cookie "+cookie);
	}
	private ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("test-store",store.getStoreRoot());
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}

	// XXX refactor
	private HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
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

	private JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		return out;
	}
	
	private String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}
	
	private String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	private JSONObject getFields(JSONObject in) throws JSONException {
		in=in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}

	private HttpTester createUser(ServletTester jetty, String JSONfile) throws IOException, JSONException, Exception{

		HttpTester out;
		JSONObject u1=new JSONObject(JSONfile);
		String userId = u1.getString("userId");
		JSONObject test = new JSONObject();
		test.put("userId", userId);
log.info(test.toString());
		/* delete user if already exists */
		out=jettyDo(jetty,"GET","/chain/users/search",test.toString());
		log.info(out.getContent());
		String itemmarker = "items";
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray(itemmarker);
		if(items.length()>0){
			for(int i=0;i<items.length();i++){
				JSONObject user = items.getJSONObject(i);
				if(user.getString("userId").equals(userId)){
					//delete record
					String csid = user.getString("csid");
					out=jettyDo(jetty,"DELETE","/chain/users/"+csid,null);
				}
			}
		}
		
		// Create a User
		out=jettyDo(jetty,"POST","/chain/users/",makeSimpleRequest(JSONfile));
		assertEquals(out.getMethod(),null);
		return out;
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
		assertEquals(201,out.getStatus());
		
		//ask to reset
		out=jettyDo(jetty,"POST","/chain/passwordreset/",user2Email);
				
		//this should fail - switch this on when we want to test with a failing token
		/*JSONObject obj = new JSONObject(out.getContent());
		Long token = Long.parseLong(obj.getString("token"));
		token -= (8*24*60*60*10000);
		obj.put("token", token);
		*/
		
		// Reset password - seems to be setting it to the same value here
		out=jettyDo(jetty,"POST","/chain/resetpassword/",out.getContent());
		
		// Read
		out=jettyDo(jetty,"GET","/chain"+id,null);
		
		// Checks User Id is unchanged
		log.info(out.getContent());
		JSONObject user2AfterReset=new JSONObject(out.getContent());
		JSONObject user2CreateCopy=new JSONObject(user2Create);
		assertEquals(user2AfterReset.getJSONObject("fields").get("userId").toString(),user2CreateCopy.get("userId").toString());
		
		// Don't know what this is aiming at (commented out already) but the much of the content is different eg status
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2CreateCopy));
		
		// Updates - changes screen name and status
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(user2Update));
		log.info(out.getContent());
		//assertEquals(200,out.getStatus());		
		
		// Read
		out=jettyDo(jetty,"GET","/chain"+id,null);
		
		// Check User Id is unchanged
		JSONObject user2AfterUpdate=new JSONObject(out.getContent());
		JSONObject user2UpdateCopy=new JSONObject(user2Update);
		assertEquals(user2AfterUpdate.getJSONObject("fields").get("userId").toString(),user2UpdateCopy.get("userId").toString());
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2UpdateCopy));
		
		// Delete
		out=jettyDo(jetty,"DELETE","/chain"+id,null);

		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
		
		
	}
	/*I think test below just duplicates the functionality in testPostGetDelete and can be removed
	@Test public void testPostAndUpdateWithRoles() throws Exception {
		ServletTester jetty=setupJetty();
		//Create
		HttpTester out=jettyDo(jetty,"POST","/chain/role/",makeSimpleRequest(roleCreate));

		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(roleCreate);

		assertEquals(one.get("roleName"), two.getString("roleName").toUpperCase());
		
		out = jettyDo(jetty, "PUT","/chain/"+id,makeSimpleRequest(roleCreate));

		assertEquals(out.getMethod(), null);
		assertEquals(200, out.getStatus());
		one = new JSONObject(getFields(out.getContent()));
		two = new JSONObject(roleCreate);
		assertEquals(one.get("roleName"), two.getString("roleName").toUpperCase());

		out=jettyDo(jetty,"GET","/chain/role",null);
		assertEquals(200,out.getStatus());

		
		
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
	}*/
	

	/**
	 * Tests CRUD for different Store Types
	 * @throws Exception
	 */
	@Test public void testMultipleStoreTypes() throws Exception {
		ServletTester jetty=setupJetty();
/*		testPostGetDelete(jetty, "/movement/", movementCreate, "movementReferenceNumber");
		testPostGetDelete(jetty, "/objects/", objectCreate, "distinguishingFeatures");
		testPostGetDelete(jetty, "/intake/", intakeCreate, "entryReason");
		testPostGetDelete(jetty, "/loanout/", loanoutCreate, "loanOutNote");
		testPostGetDelete(jetty, "/loanin/", loaninCreate, "loanInNote");
		testPostGetDelete(jetty, "/acquisition/", acquisitionCreate, "acquisitionReason");
	*/	testPostGetDelete(jetty, "/role/", roleCreate, "description");
		//testPostGetDelete(jetty, "/permission/", permissionRead, "resourceName");
		//testPostGetDelete(jetty, "/permrole/", permroleCreate, "");
	}
	/**
	 * Tests a static page
	 * @throws Exception
	 */
	@Test public void testServeStatic() throws Exception {
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/chain.properties",null);
		assertEquals(200,out.getStatus());
		assertTrue(out.getContent().contains("cspace.chain.store.dir"));
	}
	/*
	
	@Test public void testtest() throws Exception{
		ServletTester jetty=setupJetty();
		String url = "http://nightly.collectionspace.org:8180/chain/users/2c8fb223-9c62-4432-849c-12cfb7b94da5";
		
		//HttpTester out1=jettyDo(jetty,"GET","/chain/intake",null);
		String test = "{\"owners\":[],\"acquisitionSources\":[{\"acquisitionSource\":\"urn:cspace:org.collectionspace.demo:personauthority:id(0afcdc82-cc4d-43ce-8a82):person:id(8d442794-32bc-4494-8013)'Bing+Crosby'\",\"_primary\":true}],\"acquisitionDates\":[],\"objectPurchasePriceCurrency\":\"\",\"acquisitionFundingCurrency\":\"\",\"objectOfferPriceCurrency\":\"\",\"objectPurchaseOfferPriceCurrency\":\"\",\"originalObjectPurchasePriceCurrency\":\"\",\"acquisitionMethod\":\"\",\"groupPurchasePriceCurrency\":\"\",\"acquisitionReferenceNumber\":\"AR2010.3\"}";
		
		HttpTester out2=jettyDo(jetty,"GET",url,null);
		//log.info("SEARCH");
		//log.info(out1.getContent());
		log.info("RELATE");
		log.info(out2.getContent());
			
	}
	*/
	@Test public void test() throws Exception{
		String testdata = "{\"termsUsed\":[],\"relations\":{\"intake\":[{\"summary\":\"Sean Bean\",\"summarylist\":{\"currentOwner\":\"Sean Bean\",\"entryNumber\":\"CompleteIntake001\"},\"csid\":\"b369100f-ccc7-4390-aecc\",\"number\":\"CompleteIntake001\",\"relid\":\"bca548a6-1777-421a-b513\",\"relationshiptype\":\"affects\",\"recordtype\":\"intake\"},{\"summary\":\"Sean Bean\",\"summarylist\":{\"currentOwner\":\"Sean Bean\",\"entryNumber\":\"CompleteIntake001\"},\"csid\":\"58346028-dbcb-42bb-88e2\",\"number\":\"CompleteIntake001\",\"relid\":\"7378e437-c701-4c47-b789\",\"relationshiptype\":\"affects\",\"recordtype\":\"intake\"}],\"objects\":[{\"summary\":\"\",\"summarylist\":{\"nametitle\":\"\",\"objectNumber\":\"2010.1.16\"},\"csid\":\"4f8a5552-ddab-4b7f-84e6\",\"number\":\"2010.1.16\",\"relid\":\"42db720b-078c-4ef0-ae34\",\"relationshiptype\":\"affects\",\"recordtype\":\"objects\"},{\"summary\":\"\",\"summarylist\":{\"nametitle\":\"\",\"objectNumber\":\"2010.1.16\"},\"csid\":\"4f8a5552-ddab-4b7f-84e6\",\"number\":\"2010.1.16\",\"relid\":\"163c7d9d-1ce4-446e-ba58\",\"relationshiptype\":\"affects\",\"recordtype\":\"objects\"}]},\"csid\":\"4f8a5552-ddab-4b7f-84e6\",\"fields\":{\"technique\":\"\",\"inscriptionContentTranslation\":\"\",\"assocActivityNote\":\"\",\"inscriptionContentMethod\":\"\",\"inscriptionDescriptionPosition\":\"\",\"objectHistoryNote\":\"\",\"inscriptionContentInscriber\":\"\",\"viewersPersonalResponse\":\"\",\"fieldCollectionMethods\":[],\"assocEventPeoples\":[{\"_primary\":true,\"assocEventPeople\":\"\"}],\"references\":[{\"_primary\":true,\"reference\":\"\"}],\"ownershipPlace\":\"\",\"catalogNumber\":\"\",\"assocEventNote\":\"\",\"objectStatus\":\"\",\"responsibleDepartments\":[{\"_primary\":true,\"responsibleDepartment\":\"\"}],\"ownershipAccess\":\"\",\"contentOther\":\"\",\"contentPositions\":[{\"_primary\":true,\"contentPosition\":\"\"}],\"inscriptionContentPosition\":\"\",\"inscriptionContentTransliteration\":\"\",\"contentOtherType\":\"\",\"styles\":[{\"_primary\":true,\"style\":\"\"}],\"dateLatestQualifier\":\"\",\"contentObject\":\"\",\"ownershipExchangeMethod\":\"\",\"objectComponentInformation\":\"\",\"objectProductionNote\":\"\",\"ownershipExchangePriceCurrency\":\"\",\"objectProductionOrganization\":\"\",\"owners\":[{\"_primary\":true,\"owner\":\"\"}],\"assocDate\":\"\",\"inscriptionDescriptionDate\":\"\",\"inscriptionDescriptionInterpretation\":\"\",\"usageNote\":\"\",\"ownershipExchangePriceValue\":\"\",\"ownersContributionNote\":\"\",\"objectProductionPeople\":\"\",\"contentEventName\":\"\",\"objectProductionReasons\":[{\"objectProductionReason\":\"\",\"_primary\":true}],\"contentLanguages\":[{\"contentLanguage\":\"\",\"_primary\":true}],\"objectProductionPerson\":\"\",\"inscriptionContent\":\"\",\"collection\":\"\",\"assocCulturalContexts\":[{\"_primary\":true,\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"},{\"assocCulturalContext\":\"\"}],\"otherNumberList\":[{\"_primary\":true,\"otherNumber\":\"sdf\"},{\"otherNumber\":\"fff\"}],\"materialComponentNote\":\"\",\"contentDate\":\"\",\"technicalAttributeMeasurement\":\"\",\"title\":\"\",\"titleType\":\"\",\"titleTranslation\":\"\",\"inscriptionDescriptionType\":\"\",\"assocPersons\":[{\"_primary\":true,\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"},{\"assocPerson\":\"\"}],\"assocPeoples\":[{\"_primary\":true,\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"},{\"assocPeople\":\"\"}],\"dateText\":\"\",\"viewersPersonalExperience\":\"\",\"materialName\":\"\",\"dateEarliestSingle\":\"\",\"inscriptionContentScript\":\"\",\"ownersPersonalResponse\":\"\",\"ageQualifier\":\"\",\"fieldCollectionNote\":\"\",\"material\":\"\",\"viewersReferences\":[{\"_primary\":true,\"viewersReference\":\"\"}],\"assocEventNameType\":\"\",\"techniqueType\":\"\",\"fieldCollectionNumber\":\"\",\"inscriptionDescription\":\"\",\"assocEventPersons\":[{\"_primary\":true,\"assocEventPerson\":\"\"}],\"fieldCollectionPlace\":\"\",\"dateLatest\":\"\",\"fieldCollectionDate\":\"\",\"comments\":[{\"_primary\":true,\"comment\":\"\"}],\"contentDescription\":\"\",\"nhString\":\"\",\"assocEventName\":\"\",\"briefDescriptions\":[{\"_primary\":true,\"briefDescription\":\"\"}],\"objectProductionPlace\":\"\",\"viewersRole\":\"\",\"assocActivity\":\"\",\"ownersPersonalExperience\":\"\",\"assocPlaces\":[{\"_primary\":true,\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"},{\"assocPlace\":\"\"}],\"ageUnit\":\"\",\"contentOrganizations\":[{\"_primary\":true,\"contentOrganization\":\"\"}],\"materialSource\":\"\",\"ownershipCategory\":\"\",\"contentObjectType\":\"\",\"dimensionSummary\":\"\",\"assocObjectType\":\"\",\"copyNumber\":\"\",\"ownershipDates\":\"\",\"inscriptionContentInterpretation\":\"\",\"contentActivities\":[{\"_primary\":true,\"contentActivity\":\"\"}],\"age\":\"\",\"contentPersons\":[{\"_primary\":true,\"contentPerson\":\"\"}],\"assocOrganizations\":[{\"_primary\":true,\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"},{\"assocOrganization\":\"\"}],\"contentScripts\":[{\"_primary\":true,\"contentScript\":\"\"}],\"objectNumber\":\"2010.1.16\",\"colors\":[{\"_primary\":true,\"color\":\"\"}],\"ownersReferences\":[{\"_primary\":true,\"ownersReference\":\"\"}],\"dateLatestCertainty\":\"\",\"physicalDescription\":\"\",\"contentConcepts\":[{\"_primary\":true,\"contentConcept\":\"\"}],\"assocObject\":\"\",\"inscriptionContentType\":\"\",\"assocEventPlaces\":[{\"_primary\":true,\"assocEventPlace\":\"\"}],\"contentEventNameType\":\"\",\"inscriptionDescriptionInscriber\":\"\",\"assocDateNote\":\"\",\"contentPlaces\":[{\"contentPlace\":\"\",\"_primary\":true}],\"inscriptionContentLanguage\":\"\",\"phase\":\"\",\"technicalAttributeMeasurementUnit\":\"\",\"objectProductionPlaceRole\":\"\",\"titleLanguage\":\"\",\"contentNote\":\"\",\"dateEarliestSingleQualifier\":\"\",\"contentPeoples\":[{\"_primary\":true,\"contentPeople\":\"\"}],\"fieldCollectionEventName\":\"\",\"inscriptionDescriptionMethod\":\"\",\"sex\":\"\",\"objectProductionOrganizationRole\":\"\",\"recordStatus\":\"\",\"numberOfObjects\":\"\",\"technicalAttribute\":\"\",\"objectComponentName\":\"\",\"materialComponent\":\"\",\"objectProductionPersonRole\":\"\",\"objectProductionDates\":[{\"_primary\":true,\"objectProductionDate\":\"\"}],\"objectProductionPeopleRole\":\"\",\"fieldCollectionSources\":[],\"forms\":[{\"_primary\":true,\"form\":\"\"}],\"viewersContributionNote\":\"\",\"editionNumber\":\"\",\"distinguishingFeatures\":\"\",\"dateAssociation\":\"\",\"titleTranslationLanguage\":\"\",\"dateEarliestSingleCertainty\":\"\",\"fieldCollectors\":[],\"csid\":\"4f8a5552-ddab-4b7f-84e6\",\"assocEventOrganizations\":[{\"assocEventOrganization\":\"\",\"_primary\":true}],\"datePeriod\":\"\",\"ownershipExchangeNote\":\"\",\"assocConcepts\":[{\"assocConcept\":\"\",\"_primary\":true},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"},{\"assocConcept\":\"\"}],\"usage\":\"\",\"inscriptionContentDate\":\"\",\"dimensions\":[],\"objectNameGroup\":[]},\"items\":[]}";
		ServletTester jetty=setupJetty();
		
		HttpTester out2=jettyDo(jetty,"PUT","/chain/objects/4f8a5552-ddab-4b7f-84e6",testdata);
		log.info(out2.getContent());
		
	}

	/** 
	 * Test List functionality for different Store Types
	 * @throws Exception
	 */
	@Test public void testObjectList() throws Exception {
		ServletTester jetty=setupJetty();

		testLists(jetty, "objects", objectCreate, "items");
		testLists(jetty, "intake", intakeCreate, "items");
		testLists(jetty, "loanin", loaninCreate, "items");
		testLists(jetty, "loanout", loanoutCreate, "items");
		testLists(jetty, "acquisition", acquisitionCreate, "items");
		testLists(jetty, "role", roleCreate, "items");
		testLists(jetty, "movement", movementCreate, "items");
		//testLists(jetty, "permission", permissionWrite, "items");
	}
	/* XXX I don't think this is tetsing what it needs to */
	/**
	 * Writes a series of Objects then does a Read using a trailing slash
	 * and checks each object is found
	 */
	@Test public void testTrailingSlashOkayOnList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out1=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
		HttpTester out2=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
		HttpTester out3=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));
		// Read with a trailing slash
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/",null);
		assertEquals(200,out.getStatus());
		
		// Build up a list of items returned
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();
		for(int i=0;i<items.length();i++){
			files.add("/objects/"+items.getJSONObject(i).getString("csid"));
		}

		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+out1.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
		
		out=jettyDo(jetty,"DELETE","/chain"+out2.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
		
		out=jettyDo(jetty,"DELETE","/chain"+out3.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
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

		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",makeSimpleRequest(personStr));
		String person_id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("inscriptionContentInscriber",urn);
		
		//create object
		out=jettyDo(jetty,"POST","/chain/objects/",testdata.toString());
		log.info(out.getContent());
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		//read and check
		out=jettyDo(jetty,"GET","/chain"+id,null);
		JSONObject one = new JSONObject(getFields(out.getContent()));
		log.info(one.toString());
		assertEquals(one.get("inscriptionContentInscriber"), urn);
		assertEquals(one.get("de-urned-inscriptionContentInscriber"), "TEST my test person");

		//clean up
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
		
		out=jettyDo(jetty,"DELETE","/chain/vocabularies/"+person_id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/vocabularies/"+person_id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
	}
	/**
	 * Checks a vocabulary (here a person) can be linked to an object 
	 * @throws Exception
	 */
	@Test public void testTermsUsedVocab() throws Exception {
		ServletTester jetty=setupJetty();
		//create person authority to use
		String personStr = "{\"displayName\":\"TEST my test person2\"}";
		HttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person/",makeSimpleRequest(personStr));
		String person_id=out.getHeader("Location");
		log.info(out.getContent());
		JSONObject persondata = new JSONObject(out.getContent());
		String urn = persondata.getString("urn");

		//assign person authority
		JSONObject testdata = new JSONObject(urnTestJoe);
		testdata.getJSONObject("fields").put("contentPeople",urn);
		
		//create object
		out=jettyDo(jetty,"POST","/chain/objects/",testdata.toString());
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		
		// I believe the items below where copied from test above and are not needed
		//JSONObject one = new JSONObject(getFields(out.getContent()));
		
		//assertEquals(one.get("contentPeople"), urn);
		//assertEquals(one.get("de-urned-contentPeople"), "TEST my test person2");

		//get the objects linked to the vocab item
		out = jettyDo(jetty,"GET","/chain/vocabularies"+person_id,null);
		assertEquals(200, out.getStatus());
		
		//clean up
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
		
		out=jettyDo(jetty,"DELETE","/chain/vocabularies/"+person_id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/vocabularies/"+person_id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
	}

	// generic test post/get/put delete
	private void testPostGetDelete(ServletTester jetty,String uipath, String data, String testfield) throws Exception {
		HttpTester out;
		//Create
		out = jettyDo(jetty,"POST","/chain"+uipath,makeSimpleRequest(data));
		assertEquals(out.getMethod(),null);
log.info(out.getContent());
		assertEquals(201,out.getStatus());	
		String id=out.getHeader("Location");	
		//Retrieve
		out=jettyDo(jetty,"GET","/chain"+id,null);

		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(data);
		log.info(one.toString());
		log.info(two.toString());
		assertEquals(one.get(testfield).toString(),two.get(testfield).toString());

		//change
		if(!uipath.contains("permission")){
			two.put(testfield, "newvalue");
			out=jettyDo(jetty,"PUT","/chain"+id,makeRequest(two).toString());
			assertEquals(200,out.getStatus());	
			JSONObject oneA = new JSONObject(getFields(out.getContent()));
			assertEquals(oneA.get(testfield).toString(),"newvalue");
		}

		//Delete
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		
	}

	// generic Lists
	private void testLists(ServletTester jetty, String objtype, String data, String itemmarker)  throws Exception{

		HttpTester out1=jettyDo(jetty,"POST","/chain/"+objtype+"/",makeSimpleRequest(data));
		log.info(out1.getContent());
		assertEquals(201, out1.getStatus());

		// Opens a file output stream - ?? not sure why
		File storedir=new File(store.getStoreRoot(),"store");
		if(!storedir.exists())
			storedir.mkdir();
		File junk=new File(storedir,"junk");
		IOUtils.write("junk",new FileOutputStream(junk));

		/* get all objects */
		//pagination?
		HttpTester out;
		int pgSz = 100;
		int pgNum = 0;
		boolean exists = false;
		boolean end = false;
		// Page through looking for this id
		do{
			out=jettyDo(jetty,"GET","/chain/"+objtype+"/search?pageNum="+pgNum+"&pageSize="+pgSz,null);
			log.info(objtype+":"+out.getContent());
			assertEquals(200,out.getStatus());
			
			/* create list of files */

			JSONObject result=new JSONObject(out.getContent());
			JSONArray items=result.getJSONArray(itemmarker);
			Set<String> files=new HashSet<String>();
			if(items.length() > 0){
				for(int i=0;i<items.length();i++){
					files.add("/"+objtype+"/"+items.getJSONObject(i).getString("csid"));
				}
			}else{
				end = true;
			}

			exists = files.contains(out1.getHeader("Location"));
			pgNum++;
		}while(!end && !exists);
		
		assertTrue(exists);
		
		
		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+out1.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
	}
	
	
	private JSONObject createRoleWithPermission(String role, String permname, String permname2) throws Exception{

		/*
        "permissions": [
            {"resourceName": "Acquisition", "permission": "write"},
            {"resourceName": "Loan In", "permission": "read"},
        ],
		 */
		JSONArray permission = new JSONArray();
		JSONObject perm1 = new JSONObject();
		perm1.put("resourceName", permname);
		perm1.put("permission", "delete");

		JSONObject perm2 = new JSONObject();
		perm2.put("resourceName", permname2);
		perm2.put("permission", "delete");
		permission.put(perm1);
		permission.put(perm2);
		JSONObject roleJSON= new JSONObject(role);
		roleJSON.put("permissions", permission);
		return roleJSON;		
	}
	
	private JSONObject createUserWithRoles(ServletTester jetty,String user, String roleJSON) throws Exception{

		//create role
		HttpTester out = jettyDo(jetty,"POST","/chain/role/",makeSimpleRequest(roleJSON));
		log.info(out.getContent());
		JSONObject role = new JSONObject(out.getContent()).getJSONObject("fields");
		String role_id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		
		/*
        "role": [
            {"roleName": "Acquisition", "roleId": "write", "active":"active"},
        ],
		 */
		JSONArray roles = new JSONArray();
		JSONObject role1 = new JSONObject();
		role1.put("roleName", role.getString("roleName"));
		role1.put("roleId", role_id);
		role1.put("active", "active");
		
		roles.put(role1);

		JSONObject userJSON= new JSONObject(user);
		userJSON.put("role", roles);
		return userJSON;		

	}
	@Test public void testRolesPermsUI() throws Exception {

		ServletTester jetty = setupJetty();
//		create role with permissions
		JSONObject rolepermsdata = createRoleWithPermission(roleCreate,"acquisitions", "intakes"); 

		HttpTester out = jettyDo(jetty,"POST","/chain/role/",makeRequest(rolepermsdata).toString());
		assertEquals(201,out.getStatus());
		String role_id = out.getHeader("Location");

		//get role
		out=jettyDo(jetty,"GET","/chain"+role_id,null);
		assertEquals(200,out.getStatus());

		//test
		JSONObject data = new JSONObject(out.getContent());
		log.info(data.toString());
		
		//delete role		
		out=jettyDo(jetty,"DELETE","/chain"+role_id,null);
		assertEquals(200,out.getStatus());
		
		//test data
		JSONArray perms = data.getJSONObject("fields").getJSONArray("permissions");
		int test = 0;
		for(int i=0; i<perms.length();i++){
			JSONObject thisperm = perms.getJSONObject(i);
			if(thisperm.getString("resourceName").equals("intakes")){
				assertEquals("write",thisperm.getString("permission"));
				test++;
			}
			if(thisperm.getString("resourceName").equals("acquisitions")){
				assertEquals("read",thisperm.getString("permission"));
				test++;
			}
		}
		//XXX test removed until service layer have fixed there stuff
//		assertEquals("failed to find acquisitions and intakes",2,test);
	}
	
	@Test public void testUserRolesUI() throws Exception{
		ServletTester jetty = setupJetty();
		JSONObject userdata = createUserWithRoles(jetty,user88Create,roleCreate);
		JSONObject userdata2 = createUserWithRoles(jetty,user88Create,role2Create);
//create user with roles in payload
		HttpTester out = jettyDo(jetty,"POST","/chain/users/",makeRequest(userdata).toString());
		assertEquals(201,out.getStatus());
		log.info(out.getContent());

		String userid = out.getHeader("Location");
		log.info(userid);

		out=jettyDo(jetty,"GET","/chain"+userid,null);
		assertEquals(200,out.getStatus());
		log.info(out.getContent());

		String screenname = userdata2.getString("userName");
		userdata2.remove("userName");
		userdata2.put("screenName", screenname);
		
		
		out=jettyDo(jetty,"PUT","/chain"+userid,makeRequest(userdata2).toString());
		assertEquals(out.getMethod(), null);
		//assertEquals(200,out.getStatus());
		log.info("PUT"+out.getContent());

		out=jettyDo(jetty,"GET","/chain"+userid,null);
		assertEquals(200,out.getStatus());
		log.info("GET:"+out.getContent());

		JSONObject data = new JSONObject(out.getContent());
		JSONArray roles = data.getJSONObject("fields").getJSONArray("role");
		//delete roles

		//Delete the roles
		log.info(userdata.toString());
		String roles_id1 = userdata.getJSONArray("role").getJSONObject(0).getString("roleId");
		String roles_id2 = userdata2.getJSONArray("role").getJSONObject(0).getString("roleId");

		out=jettyDo(jetty,"DELETE","/chain"+roles_id1,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"DELETE","/chain"+roles_id2,null);
		assertEquals(200,out.getStatus());
		
		//delete user
		out=jettyDo(jetty,"DELETE","/chain"+userid,null);
		assertEquals(200,out.getStatus());

		//test role_1 deleted to payload
		//XXX test removed until service layer have fixed their stuff
		//assertEquals("Should only be one role, if more then it didn't delete, if less then it didn't add",1,roles.length());

		//test role_2 added to payload
		for(int i=0; i<roles.length();i++){
			JSONObject role = roles.getJSONObject(i);
			//assertEquals()
			assertEquals(role.getString("roleName"),userdata.getJSONArray("role").getJSONObject(0).getString("roleName"));
		}
	}

	/*
	@Test public void testPermRolePost() throws Exception {
		ServletTester jetty = setupJetty();
		HttpTester out;
		//Create a permission
		out = jettyDo(jetty,"POST","/chain/permrole",makeSimpleRequest(permissionCreate));
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());
		String p_id=out.getHeader("Location");
		JSONObject permissiondata = new JSONObject(out.getContent());
		String csid = permissiondata.getString("csid");

		//Add the csid of the permission in the permroleCreate
		JSONObject permrole = new JSONObject(permroleCreate);
		permrole.put("permissionId", csid);
		
		//Create a permrole
		out = jettyDo(jetty,"POST","/chain/permrole",permrole.toString());
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());	
		String id=out.getHeader("Location");	
		//Retrieve
		out=jettyDo(jetty,"GET","/chain"+id,null);

		//if(id.contains(""))
		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(permroleCreate);

		//assertEquals(one.get(testfield).toString(),two.get(testfield).toString());
		//change
		//two.put(testfield, "newvalue");
		out=jettyDo(jetty,"PUT","/chain"+id,makeRequest(two).toString());
		assertEquals(200,out.getStatus());	
		JSONObject oneA = new JSONObject(getFields(out.getContent()));

		//assertEquals(oneA.get(testfield).toString(),"newvalue");

		//Delete permrole
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		
		//Delete permission
		out=jettyDo(jetty,"DELETE","/chain"+p_id,null);
		assertEquals(200,out.getStatus());
		
	}*/

}
