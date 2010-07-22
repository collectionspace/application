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
	private final static String objectCreate = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"2\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartments\":[{\"responsibleDepartment\":\"new DEPT\"}],\"objectName\":\"new OBJNAME\"}";
	//private final static String objectCreate = "{\"accessionNumber\": \"new OBJNUM\", \"description\": \"new DESCRIPTION\", \"descInscriptionInscriber\": \"new INSCRIBER\", \"objectNumber\": \"1\", \"objectTitle\": \"new TITLE\", \"comments\": \"new COMMENTS\", \"distinguishingFeatures\": \"new DISTFEATRES\", \"responsibleDepartment\": \"new DEPT\",\"briefDescriptions\": [ { \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WAAAA\", \"primary\": \"arg\" }, { \"briefDescription\": \"WOOOOP\", \"primary\": \"bob\" } ], \"objectName\": \"new OBJNAME\"}";
	private final static String acquisitionCreate = "{\"acquisitionReason\":\"acquisitionReason\",\"acquisitionReferenceNumber\":\"acquisitionReferenceNumber\",\"acquisitionMethod\":\"acquisitionMethod\",\"owners\":[{\"owner\":\"urn:cspace:org.collectionspace.demo:orgauthority:id(4bf0090c-7d67-4d92-9370):organization:id(b09db2c1-a849-43b5-8ad1)'Bing+Crosby+Ice+Cream+Sales%2C+Inc.'\"}],\"acquisitionSources\":[{\"acquisitionSource\": \"11111\"},{\"acquisitionSource\": \"22222\"}]}";
	private final static String roleCreate = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_1_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
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

	private final static String accountroleCreate = "{ \"account\": [{ \"userId\": \"\", \"screenName\": \"\", \"accountId\": \"\" }], \"roles\": [{ \"role\": [{ \"roleName\": \"\", \"roleId\": \"\" }]}] }";

	//private final static String testStr3 = "{\"a\":\"b\",\"id\":\"***misc***\",\"objects\":\"***objects***\",\"intake\":\"***intake***\"}";
	//private final static String testStr4 = "{\"a\":\"b\",\"id\":\"MISC2009.1\",\"objects\":\"OBJ2009.1\",\"intake\":\"IN2009.1\"}";
	//private final static String testStr5 = "{\"a\":\"b\",\"id\":\"MISC2009.2\",\"objects\":\"OBJ2009.2\",\"intake\":\"IN2009.2\"}";

	private final static String user2Create = "{\"userId\": \"unittest2@collectionspace.org\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
	private final static String user2Update = "{\"userId\": \"unittest2@collectionspace.org\",\"screenName\": \"unittestzzz\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"active\"}";
	private final static String user2Email = "{\"email\": \"unittest2@collectionspace.org\", \"debug\" : true }";
	//private final static String userEmail = "{\"email\": \"unittest@collectionspace.org\", \"debug\" : true }";
	private final static String testStr10 = "{\"roleName\": \"ROLE_USERS_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	private final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"contentPeople\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
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
		// Create a User
		HttpTester out=jettyDo(jetty,"POST","/chain/users/",makeSimpleRequest(user2Create));
		assertEquals(out.getMethod(),null);
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
		JSONObject user2AfterReset=new JSONObject(out.getContent());
		JSONObject user2CreateCopy=new JSONObject(user2Create);
		assertEquals(user2AfterReset.getJSONObject("fields").get("userId").toString(),user2CreateCopy.get("userId").toString());
		
		// Don't know what this is aiming at (commented out already) but the much of the content is different eg status
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())), user2CreateCopy));
		
		// Updates - changes screen name and status
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(user2Update));
		assertEquals(200,out.getStatus());		
		
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
		testPostGetDelete(jetty, "/objects/", objectCreate, "objectName");
		testPostGetDelete(jetty, "/intake/", intakeCreate, "entryReason");
		testPostGetDelete(jetty, "/loanout/", loanoutCreate, "loanOutNote");
		testPostGetDelete(jetty, "/loanin/", loaninCreate, "loanInNote");
		testPostGetDelete(jetty, "/acquisition/", acquisitionCreate, "acquisitionReason");
		//testPostGetDelete(jetty, "/role/", roleCreate, "description");
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
		testdata.getJSONObject("fields").put("contentPeople",urn);
		
		//create object
		out=jettyDo(jetty,"POST","/chain/objects/",testdata.toString());
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		//read and check
		out=jettyDo(jetty,"GET","/chain"+id,null);
		JSONObject one = new JSONObject(getFields(out.getContent()));
		assertEquals(one.get("contentPeople"), urn);
		assertEquals(one.get("de-urned-contentPeople"), "TEST my test person");

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

		assertEquals(201,out.getStatus());	
		String id=out.getHeader("Location");	
		//Retrieve
		out=jettyDo(jetty,"GET","/chain"+id,null);

		JSONObject one = new JSONObject(getFields(out.getContent()));
		JSONObject two = new JSONObject(data);
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
	
	/**
	 * Tests Multiple Permissions
	 * @throws Exception
	 */
	//@Test 
	public void testPermissionGrouping() throws Exception {
		ServletTester jetty = setupJetty();
		HttpTester out;
		String testfield = "resourceName";
		String uipath = "/permission/";
		String[] data = {permissionRead, permissionWrite, permissionDelete, permissionNone,permission2Write, permission2None};
		List<String> ids = new ArrayList<String>();
		
		//create the permissions
		for(String s : data){
			//Create
			out = jettyDo(jetty,"POST","/chain"+uipath,makeSimpleRequest(s));
			assertEquals(out.getMethod(),null);
			assertEquals(201,out.getStatus());	
			String id=out.getHeader("Location");
			//Retrieve
			out=jettyDo(jetty,"GET","/chain"+id,null);
			JSONObject one = new JSONObject(getFields(out.getContent()));
			JSONObject two = new JSONObject(s);

			assertEquals(one.get(testfield).toString(),two.get(testfield).toString());
			
			ids.add(id);
		}
		
		//get a list of the permissions
		//pagination?
		//out=jettyDo(jetty,"GET","/chain/permission/search?query="+URLEncoder.encode(d.toString(),"UTF-8"),null);
		out=jettyDo(jetty,"GET","/chain/permission",null);
		assertEquals(200,out.getStatus());
		JSONObject json = new JSONObject(out.getContent());
		
		// Tidy up
		for(String id : ids){
			//Delete
			out=jettyDo(jetty,"DELETE","/chain"+id,null);
			assertEquals(200,out.getStatus());
		}
		
		//assertTrue(json.has("groupedPermissions"));
		
	}

	/**
	 * Compares a generated user role to one directly posted 
	 * @throws Exception
	 */
	@Test public void testUserRoles() throws Exception{
		ServletTester jetty = setupJetty();
		
		//Create a user
		HttpTester out=jettyDo(jetty,"POST","/chain/users/",makeSimpleRequest(user88Create));
		assertEquals(out.getMethod(),null);
		JSONObject user = new JSONObject(out.getContent());
		String user_id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		
		
		//Create a role
		out = jettyDo(jetty,"POST","/chain/role/",makeSimpleRequest(roleCreate));
		JSONObject role = new JSONObject(out.getContent());
		String role_id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		
		//Assign the roles to the user
		JSONObject json = new JSONObject(accountroleCreate);
		JSONArray account = json.getJSONArray("account");
		for(int i=0,il=account.length();i<il;i++){
			JSONObject accountitem = account.getJSONObject(i);
			accountitem.put("userId", getFields(user).getString("userId"));
			accountitem.put("screenName", getFields(user).getString("screenName"));
			accountitem.put("accountId", user.getString("csid"));
		}
		
		JSONArray roleslist = json.getJSONArray("roles");
		for(int i=0,il=roleslist.length();i<il;i++){
			JSONObject rolesitem = roleslist.getJSONObject(i);
			JSONArray rolesitemlist = rolesitem.getJSONArray("role");
			for(int j=0,jl=rolesitemlist.length();j<jl;j++){
				JSONObject roleitem = rolesitemlist.getJSONObject(j);
				roleitem.put("roleName", getFields(role).getString("roleName"));
				roleitem.put("roleId", role.getString("csid"));
			}
		}
		
		//create an account_role
		out = jettyDo(jetty, "POST", "/chain"+ user_id +"/userrole",makeRequest(json).toString());
		assertEquals(201, out.getStatus());
		String acrole_id = out.getHeader("Location");
		
		//Get the account_role and compare this to the originally assigned role
		out=jettyDo(jetty,"GET","/chain"+ user_id + acrole_id,null);
		assertEquals(200, out.getStatus());
		JSONObject one = new JSONObject(getFields(out.getContent()));
		//assertEquals(one.get("account").toString(),json.get("account").toString());

		Boolean testflag = false;
		JSONArray testroleslist = one.getJSONArray("roles");
		for(int i=0,il=testroleslist.length();i<il;i++){
			JSONObject rolesitem = roleslist.getJSONObject(i);
			JSONArray rolesitemlist = rolesitem.getJSONArray("role");
			for(int j=0,jl=rolesitemlist.length();j<jl;j++){
				JSONObject roleitem = rolesitemlist.getJSONObject(j);
				if(roleitem.getString("roleId").equals(role.getString("csid"))){
					testflag = true;
				}
			}
		}
		assertTrue(testflag);
		//assertEquals(one.get("roles").toString(),json.get("roles").toString());
		
		//Delete the account_role - commented out until service layer is sorted
		//out=jettyDo(jetty,"DELETE","/chain"+user_id+acrole_id,null);
		//assertEquals(200,out.getStatus());
		
		//Delete the user
		out=jettyDo(jetty,"DELETE","/chain"+user_id,null);
		assertEquals(200,out.getStatus());
		
		//Delete the roles
		out=jettyDo(jetty,"DELETE","/chain"+role_id,null);
		assertEquals(200,out.getStatus());
		
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
