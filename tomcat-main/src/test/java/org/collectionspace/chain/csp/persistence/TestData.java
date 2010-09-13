package org.collectionspace.chain.csp.persistence;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestData {
	private static final Logger log=LoggerFactory.getLogger(TestData.class);

	// Set up test data strings 
	protected final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
	"\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
	"{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
	"\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
	"\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
	"\"param\":\"type\"}]}";

	protected final static String testStr2 = "{\"accessionNumber\":\"OBJNUM\",\"description\":\"DESCRIPTION\",\"descInscriptionInscriber\":\"INSCRIBER\",\"objectNumber\":\"1\",\"objectTitle\":\"TITLE\",\"comments\":\"COMMENTS\",\"distinguishingFeatures\":\"DISTFEATURES\",\"responsibleDepartment\":\"DEPT\",\"objectName\":\"OBJNAME\"}";
	protected final static String testStr2a = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"1\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartment\":\"new DEPT\",\"objectName\":\"new OBJNAME\"}";
	protected final static Date d = new Date();
	
	protected final String loanoutCreate = getJSON("loanoutCreate.json").toString();
	//protected final static String loanoutCreate = "{\"loanPurpose\":\"research\",\"loanedObjectStatus\":\"agreed\",\"loanOutNumber\":\"LO2010.1.3\",\"loanOutNote\":\"loan out notes\",\"specialConditionsOfLoan\":\"loanout conditions\",\"lendersAuthorizationDate\":\"May 27, 2010\",\"loanedObjectStatusDate\":\"May 28, 2010\",\"loanReturnDate\":\"May 26, 2010\",\"loanOutDate\":\"May 25, 2010\",\"loanRenewalApplicationDate\":\"May 24, 2010\",\"loanedObjectStatusNote\":\"status note\"}";
	protected final static String loaninCreate = "{\"loanInNumber\":\"LI2010.1.2\",\"lendersAuthorizer\":\"lendersAuthorizer\",\"lendersAuthorizationDate\":\"lendersAuthorizationDate\",\"lendersContact\":\"lendersContact\",\"loanInContact\":\"loanInContact\",\"loanInConditions\":\"loanInConditions\",\"loanInDate\":\"loanInDate\",\"loanReturnDate\":\"loanReturnDate\",\"loanRenewalApplicationDate\":\"loanRenewalApplicationDate\",\"loanInNote\":\"loanInNote\",\"loanPurpose\":\"loanPurpose\"}";
	protected final static String intakeCreate = "{\"normalLocation\": \"normalLocationX\",\"fieldCollectionEventName\": \"fieldCollectionEventNameX\",\"earliestDateCertainty\": \"earliestDateCertaintyX\",\"earliestDate\": \"earliestDateX\",\"latestDate\": \"latestDateX\",\"entryNumber\": \"entryNumberX\",\"insurancePolicyNumber\": \"insurancePolicyNumberX\",\"depositorsRequirements\": \"depositorsRequirementsX\",\"entryReason\": \"entryReasonX\",\"earliestDateQualifier\": \"earliestDateQualifierX\"}";
	protected final static String objectCreate = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"2\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartments\":[{\"responsibleDepartment\":\"new DEPT\"}],\"objectNameGroup\": [{ \"objectName\": \"new OBJNAME\",}]}";
	//protected final static String objectCreate = "{\"accessionNumber\": \"new OBJNUM\", \"description\": \"new DESCRIPTION\", \"descInscriptionInscriber\": \"new INSCRIBER\", \"objectNumber\": \"1\", \"objectTitle\": \"new TITLE\", \"comments\": \"new COMMENTS\", \"distinguishingFeatures\": \"new DISTFEATRES\", \"responsibleDepartment\": \"new DEPT\",\"briefDescriptions\": [ { \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WAAAA\", \"primary\": \"arg\" }, { \"briefDescription\": \"WOOOOP\", \"primary\": \"bob\" } ], \"objectName\": \"new OBJNAME\"}";
	protected final static String movementCreate = "{\"normalLocation\":\"blah\",\"movementContact\":\"blah\",\"movementReferenceNumber\":\"MV2010.99\",\"currentLocationFitness\":\"blah\",\"removalDate\":\"2012-04-29\",\"locationDate\":\"2012-01-29\",\"plannedRemovalDate\":\"2012-03-29\",\"movementMethods\":[{\"movementMethod\":\"blah\"}],\"movementNote\":\"blah\",\"reasonForMove\":\"blah\",\"currentLocation\":\"blah\",\"currentLocationNote\":\"blah\"}";
	protected final static String acquisitionCreate = "{\"acquisitionReason\":\"acquisitionReason\",\"acquisitionReferenceNumber\":\"acquisitionReferenceNumber\",\"acquisitionMethod\":\"acquisitionMethod\",\"owners\":[{\"owner\":\"urn:cspace:org.collectionspace.demo:orgauthority:id(4bf0090c-7d67-4d92-9370):organization:id(b09db2c1-a849-43b5-8ad1)'Bing+Crosby+Ice+Cream+Sales%2C+Inc.'\"}],\"acquisitionSources\":[{\"acquisitionSource\": \"11111\"},{\"acquisitionSource\": \"22222\"}]}";
	protected final static String roleCreate = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_1_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	protected final static String role2Create = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_2_TEST_" + d.toString() + "\", \"description\": \"this role is also for test users\"}";
	final static String personCreate = "{\"fields\":{\"displayName\":\"TEST_PERSON4_display\"}}";
	

	protected final static String permissionDelete = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]}, {\"action\": [{ \"name\": \"DELETE\" }]} ], \"effect\": \"PERMIT\" }";
	protected final static String permissionRead = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"READ\" }]} ], \"effect\": \"PERMIT\" }";
	protected final static String permissionWrite = "{ \"resourceName\": \"intake\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	protected final static String permissionNone = "{ \"resourceName\": \"intake\", \"actions\": [], \"effect\": \"PERMIT\" }";
	protected final static String permission2Write = "{ \"resourceName\": \"loanin\", \"actions\": [ {\"action\": [{ \"name\": \"CREATE\" }]}, {\"action\": [{ \"name\": \"READ\" }]}, {\"action\": [{ \"name\": \"UPDATE\" }]} ], \"effect\": \"PERMIT\" }";
	protected final static String permission2None = "{ \"resourceName\": \"loanin\", \"actions\": [], \"effect\": \"PERMIT\" }";
	
	protected final static String permroleCreate = "{ \"permissions\": [ {\"recordType\": \"Object Cataloging\", \"permission\": \"write\"}, {\"recordType\": \"Intake\", \"permission\": \"write\"}, {\"recordType\": \"Acquisition\", \"permission\": \"write\"}, {\"recordType\": \"Loan In\", \"permission\": \"read\"}, {\"recordType\": \"Loan out\", \"permission\": \"read\"}] }";

	protected final static String accountroleCreate = "{ \"account\": { \"userId\": \"\", \"screenName\": \"\", \"accountId\": \"\" },  \"role\": [{ \"roleName\": \"\", \"roleId\": \"\" }] }";

	//protected final static String testStr3 = "{\"a\":\"b\",\"id\":\"***misc***\",\"objects\":\"***objects***\",\"intake\":\"***intake***\"}";
	//protected final static String testStr4 = "{\"a\":\"b\",\"id\":\"MISC2009.1\",\"objects\":\"OBJ2009.1\",\"intake\":\"IN2009.1\"}";
	//protected final static String testStr5 = "{\"a\":\"b\",\"id\":\"MISC2009.2\",\"objects\":\"OBJ2009.2\",\"intake\":\"IN2009.2\"}";

	protected final static String user2Create = "{\"userId\": \"unittest2@collectionspace.org\",\"screenName\": \"unittestzzz\",\"userName\": \"unittest2@collectionspace.org\",\"password\": \"testpassword\",\"email\": \"unittest2@collectionspace.org\",\"status\": \"inactive\"}";
	protected final String user2Update = addData("userUpdate.json").toString();
	protected final String user2Email = addData("userEmail.json").toString();
	//protected final static String userEmail = "{\"email\": \"unittest@collectionspace.org\", \"debug\" : true }";
	protected final static String testStr10 = "{\"roleName\": \"ROLE_USERS_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	protected final static String urnTestJoe = "{\"fields\":{\"responsibleDepartment\":\"\",\"dimensionMeasurementUnit\":\"\",\"objectNumber\":\"TestObject\",\"title\":\"Test Title for urn test object\",\"objectName\":\"Test Object for urn test object\",\"inscriptionContentInscriber\":\"urn:cspace:org.collectionspace.demo:personauthority:id(de0d959d-2923-4123-830d):person:id(8a6bf9d8-6dc4-4c78-84e9)'Joe+Adamson'\"},\"csid\":\"\"}";
	
	protected final String user88Create  = addData("userCreate.json", "userId").toString();
		
	
	
	
	private JSONObject addData(String jsonfile){
		JSONObject userObj = getJSON(jsonfile);
		return userObj;
	}	
	
	private JSONObject addData(String jsonfile, String field){
		JSONObject userObj = getJSON(jsonfile);
		try {
			String stuff = userObj.getString(field);
			userObj.put(field, stuff+d.toString());
		} catch (JSONException e) {
			errored(e);
		}
		return userObj;
	}
	
	private void errored(Exception e){
		log.error("ERROR occured"+e.getMessage());
		org.junit.Assert.fail("ERROR occured"+e.getMessage());
	}
	
	private JSONObject getJSON(String in) {
		try {
			String path=TestData.class.getPackage().getName().replaceAll("\\.","/");
			InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		
			assertNotNull(stream);
			String data;
			data = IOUtils.toString(stream,"UTF-8");
			stream.close();	
			return new JSONObject(data);
		} catch (IOException e) {
			errored(e);
		} catch (JSONException e) {
			errored(e);
		}
		return null;
	}
	
}
