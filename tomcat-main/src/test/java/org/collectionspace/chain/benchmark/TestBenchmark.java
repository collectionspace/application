package org.collectionspace.chain.benchmark;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

import com.carrotsearch.junitbenchmarks.*;
import com.carrotsearch.junitbenchmarks.h2.*;

@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "benchmark-lists")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20)
public class TestBenchmark extends AbstractBenchmark {

	private static String cookie;
	private static ServletTester jetty;
	private HttpTester response;
	private JSONObject generated;
	private JSONObject comparison;

	private final static Date d = new Date();
	
	private final static String loanoutCreate = "{\"loanPurpose\":\"research\",\"loanedObjectStatus\":\"agreed\",\"loanOutNumber\":\"LO2010.1.3\",\"loanOutNote\":\"loan out notes\",\"specialConditionsOfLoan\":\"loanout conditions\",\"lendersAuthorizationDate\":\"May 27, 2010\",\"loanedObjectStatusDate\":\"May 28, 2010\",\"loanReturnDate\":\"May 26, 2010\",\"loanOutDate\":\"May 25, 2010\",\"loanRenewalApplicationDate\":\"May 24, 2010\",\"loanedObjectStatusNote\":\"status note\"}";
	private final static String loaninCreate = "{\"loanInNumber\":\"LI2010.1.2\",\"lendersAuthorizer\":\"lendersAuthorizer\",\"lendersAuthorizationDate\":\"lendersAuthorizationDate\",\"lendersContact\":\"lendersContact\",\"loanInContact\":\"loanInContact\",\"loanInConditions\":\"loanInConditions\",\"loanInDate\":\"loanInDate\",\"loanReturnDate\":\"loanReturnDate\",\"loanRenewalApplicationDate\":\"loanRenewalApplicationDate\",\"loanInNote\":\"loanInNote\",\"loanPurpose\":\"loanPurpose\"}";
	private final static String intakeCreate = "{\"normalLocation\": \"normalLocationX\",\"fieldCollectionEventName\": \"fieldCollectionEventNameX\",\"earliestDateCertainty\": \"earliestDateCertaintyX\",\"earliestDate\": \"earliestDateX\",\"latestDate\": \"latestDateX\",\"entryNumber\": \"entryNumberX\",\"insurancePolicyNumber\": \"insurancePolicyNumberX\",\"depositorsRequirements\": \"depositorsRequirementsX\",\"entryReason\": \"entryReasonX\",\"earliestDateQualifier\": \"earliestDateQualifierX\"}";
	private final static String objectCreate = "{\"accessionNumber\":\"new OBJNUM\",\"description\":\"new DESCRIPTION\",\"descInscriptionInscriber\":\"new INSCRIBER\",\"objectNumber\":\"2\",\"objectTitle\":\"new TITLE\",\"comments\":\"new COMMENTS\",\"distinguishingFeatures\":\"new DISTFEATURES\",\"responsibleDepartments\":[{\"responsibleDepartment\":\"new DEPT\"}],\"objectName\":\"new OBJNAME\"}";
	//private final static String objectCreate = "{\"accessionNumber\": \"new OBJNUM\", \"description\": \"new DESCRIPTION\", \"descInscriptionInscriber\": \"new INSCRIBER\", \"objectNumber\": \"1\", \"objectTitle\": \"new TITLE\", \"comments\": \"new COMMENTS\", \"distinguishingFeatures\": \"new DISTFEATRES\", \"responsibleDepartment\": \"new DEPT\",\"briefDescriptions\": [ { \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WOOOO\" },{ \"briefDescription\": \"WAAAA\", \"primary\": \"arg\" }, { \"briefDescription\": \"WOOOOP\", \"primary\": \"bob\" } ], \"objectName\": \"new OBJNAME\"}";
	private final static String acquisitionCreate = "{\"acquisitionReason\":\"acquisitionReason\",\"acquisitionReferenceNumber\":\"acquisitionReferenceNumber\",\"acquisitionMethod\":\"acquisitionMethod\",\"owners\":[{\"owner\":\"urn:cspace:org.collectionspace.demo:orgauthority:id(4bf0090c-7d67-4d92-9370):organization:id(b09db2c1-a849-43b5-8ad1)'Bing+Crosby+Ice+Cream+Sales%2C+Inc.'\"}],\"acquisitionSources\":[{\"acquisitionSource\": \"11111\"},{\"acquisitionSource\": \"22222\"}]}";
	private final static String roleCreate = "{\"roleGroup\":\"roleGroup\", \"roleName\": \"ROLE_1_TEST_" + d.toString() + "\", \"description\": \"this role is for test users\"}";
	
	
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

	@Before public void setup() throws IOException, CSPDependencyException {
		File tmp=new File(tmpdir());
		File dir=new File(tmp,"ju-cspace");
		if(dir.exists())
			rm_r(dir);
		if(!dir.exists())
			dir.mkdir();
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
	private static void login(ServletTester tester) throws IOException, Exception {
		//HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		String test = "{\"userid\":\"test@collectionspace.org\",\"password\":\"testtest\"}";
		HttpTester out=jettyDo(tester,"POST","/chain/login/",test);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
	}	
	private static HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
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
	private static ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
		return tester;
	}
 
    /** Prepare random numbers for tests. */
    @BeforeClass
    public static void prepare() throws Exception
    {
    	jetty=setupJetty();
    }


	@Test public void CRUDObject() throws Exception {
		testCRUD(jetty, "/objects/", objectCreate, "objectName");
	}
	
	@Test public void CRUDintake() throws Exception {
		testCRUD(jetty, "/intake/", intakeCreate, "entryReason");
		
	}
	@Test public void CRUDloanin() throws Exception {
		testCRUD(jetty, "/loanin/", loaninCreate, "loanInNote");
		
	}
	@Test public void CRUDloanout() throws Exception {
		testCRUD(jetty, "/loanout/", loanoutCreate, "loanOutNote");
		
	}
	@Test public void CRUDacquisition() throws Exception {
		testLists(jetty, "/acquisition/", acquisitionCreate, "acquisitionReason");
	}


	@Test public void ListObject() throws Exception {
		testLists(jetty, "objects", objectCreate, "items");
	}
	
	@Test public void List20Intake() throws Exception {
		testLists(jetty, "intake", intakeCreate, "items");
		
	}
	@Test public void List20Loanin() throws Exception {
		testLists(jetty, "loanin", loaninCreate, "items");
		
	}
	@Test public void List20Loanout() throws Exception {
		testLists(jetty, "loanout", loanoutCreate, "items");
		
	}
	@Test public void List20Acquisition() throws Exception {
		testLists(jetty, "acquisition", acquisitionCreate, "items");
	}

	@Test public void List20Role() throws Exception {
		testLists(jetty, "role", roleCreate, "items");
	}

	@Test public void UISpecObjects() throws Exception {
		response=jettyDo(jetty,"GET","/chain/objects/uispec",null);
	}
	@Test public void UISpecIntake() throws Exception {
		response=jettyDo(jetty,"GET","/chain/intake/uispec",null);
	}
	@Test public void UISpecAcquisition() throws Exception {
		response=jettyDo(jetty,"GET","/chain/acquisition/uispec",null);
	}
	@Test public void UISpecPerson() throws Exception {
		response=jettyDo(jetty,"GET","/chain/person/uispec",null);
	}
	@Test public void UISpecOrganization() throws Exception {
		response=jettyDo(jetty,"GET","/chain/organization/uispec",null);
	}

	@Test public void UISpecObjectTab() throws Exception {
		response=jettyDo(jetty,"GET","/chain/object-tab/uispec",null);
	}
	@Test public void UISpecLoanin() throws Exception {
		response=jettyDo(jetty,"GET","/chain/loanin/uispec",null);
	}
	@Test public void UISpecLoanout() throws Exception {
		response=jettyDo(jetty,"GET","/chain/loanout/uispec",null);
	}
	@Test public void UISpecUsers() throws Exception {
		response=jettyDo(jetty,"GET","/chain/users/uispec",null);
	}

	@Test public void UISpecRole() throws Exception {
		response=jettyDo(jetty,"GET","/chain/role/uispec",null);
	}

	@Test public void UISpecPermission() throws Exception {
		response=jettyDo(jetty,"GET","/chain/permission/uispec",null);
	}

	@Test public void UISpecFindEdit() throws Exception {
		response=jettyDo(jetty,"GET","/chain/find-edit/uispec",null);
	}

	
	private void testCRUD(ServletTester jetty,String uipath, String data, String testfield) throws Exception {
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
	private void testLists(ServletTester jetty, String objtype, String data, String itemmarker)  throws Exception{

		HttpTester out1=jettyDo(jetty,"POST","/chain/"+objtype+"/",makeSimpleRequest(data));
		assertEquals(201, out1.getStatus());

		
		HttpTester out;
		int pgSz = 20;
		int pgNum = 0;
		out=jettyDo(jetty,"GET","/chain/"+objtype+"/search?pageNum="+pgNum+"&pageSize="+pgSz,null);
		assertEquals(200,out.getStatus());
			
		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+out1.getHeader("Location"),null);
		assertEquals(200,out.getStatus());
	}
}
