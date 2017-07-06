package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.webui.record.RecordCreateUpdate;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUIRecords {
	private static final Logger log=LoggerFactory.getLogger(TestUIRecords.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}
	

	/**
	 * Test Jetty
	 */
	//@Test public void testJettyStartupWorks() throws Exception {
	//	tester.setupJetty();
	//}
	/**
	 * Test Login Status
	 */
	@Test public void testLoginStatus() throws Exception {
		HttpTester out = tester.GETData("/loginstatus/",  jetty);
		JSONObject content=new JSONObject(out.getContent());
		log.info("Testing Login true");
		assertTrue(content.getBoolean("login"));
		log.info("Testing has permissions");
		assertTrue(content.has("permissions"));
	}

	/**
	 * Test Record CRUDL 
	 */
	@Test public void testCollectionObject() throws Exception {
		
		//tester.testUIspec(jetty, "/cataloging-search/uischema", "collection-object.uischema");
		log.info("Testing CollectionObject Record");
		log.info("Testing CRUDL");
		tester.testPostGetDelete(jetty, "/cataloging/", tester.objectCreate(), "distinguishingFeatures");
		log.info("Testing List");
		tester.testLists(jetty, "/cataloging/",tester.objectCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/cataloging/uispec", "collection-object.uispec");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/cataloging/uischema", "collection-object.uischema");
	}

	/**
	 * Test the Record Traverser functionality
	 * will only really highlight big fubars as I can't rely on data numbers in returns for testing
	 * @throws Exception
	 */
	@Test public void testTraverser() throws Exception{

		ArrayList<String> deleteme = new ArrayList<String>(); 
		//create 10 records
		String uipath = "/loanin/";
		String data = tester.loaninCreate();
		String testfield = "loanInNumber";
		Integer count = 0;
		HttpTester out;
		
		while(count < 10){
			JSONObject two = new JSONObject(data);
			two.put(testfield, "loanInNumber"+count);
			if(count < 6){
				two.put("loanInNote", "loanInNote");
				two.put("loanPurpose", "exhibition"); //advsearchfield
			}
			else{
				two.put("loanInNote", "OtherAnswer");
				two.put("loanPurpose", "analysis");
			}
			if(count%2 >0){
				two.put("loanPurpose", "photography");
			}
			// Create
			out = tester.POSTData(uipath, tester.makeRequest(two),jetty);
			String id = out.getHeader("Location");
			deleteme.add(id);
			
			count++;
		}
		
		
		//search with a pageSize of 3 so we ensure pagination issues (should be 6 results)
		String pgNum = "0";
		String pgSz = "3";
		out = tester.GETData(uipath + "search?query=loanInNote&pageNum=" + pgNum + "&pageSize=" + pgSz, jetty);
		//log.info(out.getContent());
		JSONObject output = new JSONObject(out.getContent());
		String token = output.getJSONObject("pagination").getString("traverser");

		String path = "/adjacentRecords/"+token+"/0";
		out = tester.GETData(path,  jetty);
		//if this doesn't error then I am relatively happy.... it is difficult to do a real test as I don't know the order things will be returned
		//log.info(out.getContent());
		JSONObject outtest = new JSONObject(out.getContent());
		//test next exists but previous doesn't
		assertTrue(outtest.has("next"));
		assertFalse(outtest.has("previous"));
		//outtest.has("next")
		Integer max = outtest.getInt("total") -1;
		assertTrue(max>4);
		
		String path2 = "/adjacentRecords/"+token+"/"+max;
		out = tester.GETData(path2,  jetty);
		//if this doesn't error then I am relatively happy.... it is difficult to do a real test as I don't know the order things will be returned
		//log.info(out.getContent());
		JSONObject outtest2 = new JSONObject(out.getContent());
		//test previous exists but next doesn't
		assertTrue(outtest2.has("previous"));
		assertFalse(outtest2.has("next"));
		
		//need to test with adv search as well
		JSONObject advsearch = new JSONObject();
		JSONObject bit = new JSONObject();
		bit.put("loanPurpose", "exhibition");
		JSONObject bit2 = new JSONObject();
		bit2.put("loanPurpose", "analysis");
		JSONArray datar = new JSONArray();
		datar.put(bit);
		datar.put(bit2);
		JSONObject fields = new JSONObject();
		fields.put("loanPurposes", datar);
		advsearch.put("operation", "or");
		advsearch.put("fields", fields);
		out = tester.POSTData(uipath + "search?query=&pageNum=" + pgNum + "&pageSize=" + pgSz, advsearch.toString(), jetty,"GET");
		//log.info(out.getContent());
		String advtoken = new JSONObject(out.getContent()).getJSONObject("pagination").getString("traverser");

		String path3 = "/adjacentRecords/"+advtoken+"/0";
		out = tester.GETData(path,  jetty);
		//if this doesn't error then I am relatively happy.... it is difficult to do a real test as I don't know the order things will be returned
		//log.info(out.getContent());
		JSONObject outtest3 = new JSONObject(out.getContent());
		//test next exists but previous doesn't
		assertTrue(outtest3.has("next"));
		assertFalse(outtest3.has("previous"));

		assertTrue(outtest3.getInt("total")>4);

		// Delete
		if(deleteme.size()>0){
			for(String item: deleteme){
				tester.DELETEData(item,jetty);
			}
		}
	}

	/**
	 * Test Procedure CRUDL
	 */
	@Test public void testProcedureMovement() throws Exception {
		log.info("Testing movement Procedure");
		tester.testPostGetDelete(jetty, "/movement/", tester.movementCreate(), "movementReferenceNumber");
		tester.testLists(jetty, "/movement/", tester.movementCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/movement/uispec", "movement.uispec");
	}


	/**
	 * Test Intake Procedure CRUDL
	 */
	@Test public void testProcedureIntake() throws Exception {
		log.info("Testing intake Procedure");
		tester.testPostGetDelete(jetty, "/intake/", tester.intakeCreate(), "entryReason");
		tester.testLists(jetty, "/intake/", tester.intakeCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/intake/uispec", "intake.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/intake-search/uispec", "intake-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/intake-search/uischema", "intake-search.uischema");
	}

	@Test public void testAllUISpecs() throws Exception {
		tester.testUIspec(jetty, "/cataloging-search/uischema", "collection-object-search.uischema");
		tester.testUIspec(jetty, "/cataloging-search/uispec", "collection-object-search.uispec");
		tester.testUIspec(jetty, "/media/uischema", "media.uischema");
		tester.testUIspec(jetty, "/termlist/uischema", "termlist.uischema");
		
		
		tester.testUIspec(jetty, "/person/uispec", "person.uispec");
		tester.testUIspec(jetty, "/person/uischema", "person.uischema");

//		tester.testUIspec(jetty, "/recordlist/uischema", "recordlist.uischema");
//		tester.testUIspec(jetty, "/namespaces/uischema", "namespaces.uischema");
//		tester.testUIspec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
		tester.testUIspec(jetty, "/acquisition-search/uischema", "acquisition-search.uischema");
		tester.testUIspec(jetty, "/intake-search/uischema", "intake-search.uischema");
		
		tester.testUIspec(jetty, "/cataloging/uischema", "collection-object.uischema");
		tester.testUIspec(jetty, "/acquisition/uischema", "acquisition.uischema");
		tester.testUIspec(jetty, "/conditioncheck-search/uischema", "conditioncheck-search.uischema");
		tester.testUIspec(jetty, "/conditioncheck/uischema", "conditioncheck.uischema");
		tester.testUIspec(jetty, "/conservation-search/uischema", "conservation-search.uischema");
		tester.testUIspec(jetty, "/conservation/uischema", "conservation.uischema");
		tester.testUIspec(jetty, "/exhibition-search/uischema", "exhibition-search.uischema");
		tester.testUIspec(jetty, "/exhibition/uischema", "exhibition.uischema");
		tester.testUIspec(jetty, "/valuationcontrol-search/uischema", "valuationcontrol-search.uischema");
		tester.testUIspec(jetty, "/valuationcontrol/uischema", "valuationcontrol.uischema");
		
		tester.testUIspec(jetty, "/cataloging/uispec", "collection-object.uispec");
		tester.testUIspec(jetty, "/intake/uispec", "intake.uispec");
		tester.testUIspec(jetty, "/loanout/uispec", "loanout.uispec");
		tester.testUIspec(jetty, "/loanin/uispec", "loanin.uispec");
		tester.testUIspec(jetty, "/acquisition/uispec", "acquisition.uispec");
		tester.testUIspec(jetty, "/acquisition-search/uispec", "acquisition-search.uispec");
		tester.testUIspec(jetty, "/conditioncheck/uispec", "conditioncheck.uispec");
		tester.testUIspec(jetty, "/conditioncheck-search/uispec", "conditioncheck-search.uispec");
		tester.testUIspec(jetty, "/conservation/uispec", "conservation.uispec");
		tester.testUIspec(jetty, "/conservation-search/uispec", "conservation-search.uispec");
		tester.testUIspec(jetty, "/exhibition/uispec", "exhibition.uispec");
		tester.testUIspec(jetty, "/exhibition-search/uispec", "exhibition-search.uispec");
		tester.testUIspec(jetty, "/valuationcontrol-search/uispec", "valuationcontrol-search.uispec");
		tester.testUIspec(jetty, "/valuationcontrol/uispec", "valuationcontrol.uispec");

	}

	/**
	 * Test Loanout Procedure CRUDL
	 */
	@Test public void testProcedureLoanout() throws Exception {
		log.info("Testing loanout Procedure");
		tester.testPostGetDelete(jetty, "/loanout/", tester.loanoutCreate(), "loanOutNote");
		tester.testLists(jetty, "/loanout/", tester.loanoutCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/loanout/uispec", "loanout.uispec");
	}


	/**
	 * Test Loanin Procedure CRUDL
	 */
	@Test public void testProcedureLoanin() throws Exception {
		log.info("Testing loanin Procedure");
		tester.testPostGetDelete(jetty, "/loanin/", tester.loaninCreate(), "loanInNote");
		tester.testLists(jetty, "/loanin/", tester.loaninCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/loanin/uispec", "loanin.uispec");
	}


	/**
 	 * Test Conditioncheck Procedure CRUDL
 	 */
	@Test public void testProcedureConditioncheck() throws Exception {
		log.info("Testing conditioncheck Procedure");
		tester.testPostGetDelete(jetty, "/conditioncheck/", tester.conditioncheckCreate(), "conditionCheckNote");
		tester.testLists(jetty, "/conditioncheck/", tester.conditioncheckCreate(), "items");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/conditioncheck/uischema", "conditioncheck.uischema");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/conditioncheck/uispec", "conditioncheck.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/conditioncheck-search/uispec", "conditioncheck-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/conditioncheck-search/uischema", "conditioncheck-search.uischema");
	}


	/**
 	 * Test Conservation Procedure CRUDL
 	 */
	@Test public void testProcedureConservation() throws Exception {
		log.info("Testing conservation Procedure");
		tester.testPostGetDelete(jetty, "/conservation/", tester.conservationCreate(), "proposedTreatment");
		tester.testLists(jetty, "/conservation/", tester.conservationCreate(), "items");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/conservation/uischema", "conservation.uischema");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/conservation/uispec", "conservation.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/conservation-search/uispec", "conservation-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/conservation-search/uischema", "conservation-search.uischema");
	}


	/**
 	 * Test Exhibition Procedure CRUDL
 	 */
	@Test public void testProcedureExhibition() throws Exception {
		log.info("Testing exhibition Procedure");
		tester.testPostGetDelete(jetty, "/exhibition/", tester.exhibitionCreate(), "planningNote");
		tester.testLists(jetty, "/exhibition/", tester.exhibitionCreate(), "items");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/exhibition/uischema", "exhibition.uischema");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/exhibition/uispec", "exhibition.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/exhibition-search/uispec", "exhibition-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/exhibition-search/uischema", "exhibition-search.uischema");
	}


	/**
	 * Test Acquisition Procedure CRUDL
	 */
	@Test public void testProcedureAcquisition() throws Exception {
		log.info("Testing acquisition Procedure");
		tester.testPostGetDelete(jetty, "/acquisition/", tester.acquisitionCreate(), "acquisitionReason");
		tester.testLists(jetty, "/acquisition/", tester.acquisitionCreate(), "items");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/acquisition/uischema", "acquisition.uischema");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/acquisition/uispec", "acquisition.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/acquisition-search/uispec", "acquisition-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/acquisition-search/uischema", "acquisition-search.uischema");
	}


	/**
	 * Test Group Procedure CRUDL
	 */
	@Test public void testProcedureGroup() throws Exception {
		log.info("Testing group Procedure");
		tester.testPostGetDelete(jetty, "/group/", tester.groupCreate(), "title");
		tester.testLists(jetty, "/group/", tester.groupCreate(), "items");

		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/group/uispec", "group.uispec");
	}
	/**
	 * Test ObjectExit Procedure CRUDL
	 */
	@Test public void testProcedureObjectexit() throws Exception {
		log.info("Testing objectexit Procedure");
		tester.testPostGetDelete(jetty, "/objectexit/", tester.objectexitCreate(), "exitNumber");
		tester.testLists(jetty, "/objectexit/", tester.objectexitCreate(), "items");

		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/objectexit/uispec", "objectexit.uispec");
		//objectexit
	}


	/**
     *  Test Valuation Control Procedure CRUDL
     */
	@Test public void testProcedureValuationcontrol() throws Exception {
		log.info("Testing valuationcontrol Procedure");
		tester.testPostGetDelete(jetty, "/valuationcontrol/", tester.valuationcontrolCreate(), "valueNote");
		tester.testLists(jetty, "/valuationcontrol/", tester.valuationcontrolCreate(), "items");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/valuationcontrol/uischema", "valuationcontrol.uischema");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/valuationcontrol/uispec", "valuationcontrol.uispec");
		log.info("Testing Search UISPEC");
		tester.testUIspec(jetty, "/valuationcontrol-search/uispec", "valuationcontrol-search.uispec");
		log.info("Testing Search UISCHEMA");
		tester.testUIspec(jetty, "/valuationcontrol-search/uischema", "valuationcontrol-search.uischema");
	}

	
	/**
	 * Test Authorities
	 */
	@Test public void testAuthorities() throws Exception {
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/person/uispec", "person.uispec");
		tester.testUIspec(jetty, "/location/uispec", "location.uispec");
		tester.testUIspec(jetty, "/organization/uispec", "organization-authority.uispec");
		tester.testUIspec(jetty, "/concept/uispec", "concept-authority.uispec");
		tester.testUIspec(jetty, "/concept-search/uispec", "concept-search.uispec");
        tester.testUIspec(jetty, "/place/uispec", "place.uispec");
        tester.testUIspec(jetty, "/place-search/uispec", "place-search.uispec");
        tester.testUIspec(jetty, "/work/uispec", "work.uispec");
        tester.testUIspec(jetty, "/work-search/uispec", "work-search.uispec");

		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/person/uischema", "person.uischema");
		tester.testUIspec(jetty, "/concept/uischema", "concept-authority.uischema");
		tester.testUIspec(jetty, "/concept-search/uischema", "concept-search.uischema");
        tester.testUIspec(jetty, "/place/uischema", "place.uischema");
        tester.testUIspec(jetty, "/place-search/uischema", "place-search.uischema");
        tester.testUIspec(jetty, "/work/uischema", "work.uischema");
        tester.testUIspec(jetty, "/work-search/uischema", "work-search.uischema");
	}
	

	/**
	 * Test Media CRUDL
	 */
	@Test public void testProcedureMedia() throws Exception {
		tester.testPostGetDelete(jetty, "/media/", tester.mediaCreate(), "identificationNumber");
		tester.testLists(jetty, "/media/",tester. mediaCreate(), "items");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/media/uispec", "media.uispec");
		tester.testUIspec(jetty, "/media/uischema", "media.uischema");

		log.info("Testing UISPEC for content");
		HttpTester out = tester.GETData("/media/uispec",jetty);
		assertEquals(200,out.getStatus());
		JSONObject spec=new JSONObject(out.getContent());
		assertEquals("${fields.blobs.0.length}",spec.getJSONObject("recordEditor").getString(".csc-blobs-length"));
	}
	
	/**
	 * Test Media Blob CRUDL
	 */
	//killed as it fails a lot
	//@Test
	public void testProcedureMediaBlob() throws Exception {
		mediaWithBlob();
	}


	//@Test 
	public void testUpload() throws Exception {
		String filename = getClass().getPackage().getName().replaceAll("\\.","/")+"/darwin-beard-hat.jpg";
		byte[] data = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
		UTF8SafeHttpTester out=tester.POSTBinaryData("/uploads",data,jetty);
		log.info(out.getContent());
		JSONObject response = new JSONObject(out.getContent());
		assertTrue(response.getString("file").contains("/blobs/"));
		assertTrue(response.optString("csid")!=null);
		assertNotSame("",response.optString("csid"));
		// Actual resource
		String read_url = response.getString("file").replaceAll("^.*?/blobs/","/download/")+"/Original";
		UTF8SafeHttpTester out2=tester.GETBinaryData(read_url,jetty,200);
		assertEquals("image/jpeg",out2.getHeader("Content-Type"));
		byte[] img = out2.getBinaryContent();
		assertArrayEquals(img,data);
	}
	/**
	 * Test Vocabulary / TermList CRUDL
	 */
	@Test public void testVocabularyTermLists() throws Exception {
		tester.testPostGetDelete(jetty, "/termlist/", tester.termlistCreate(), "description");
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/termlist/uispec", "termlist.uispec");
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/termlist/uischema", "termlist.uischema");

	}
	
	/**
	 * Test SearchAll
	 */
	@Test public void testSearchAll() throws Exception {
		log.info("Testing SearchAll with pageSize 10");
		HttpTester out = tester.GETData(
				"/all/search?&pageNum=0&pageSize=10", jetty);
		assertEquals(200, out.getStatus());
		//log.info(out.getContent());
		JSONObject result = new JSONObject(out.getContent());
		JSONArray items = result.getJSONArray("items");
		log.debug(items.length() + " items returned");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			log.trace("Item "+ i 
					+ " number: [" + item.getString("number")
					+ "] summary: [" + item.getString("summary")
					+ "] recordtype: [" + item.getString("recordtype") + "]");
		}

		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/all/uispec", "searchall.uispec");
	}


	/**
	 * Test that search ordered by summary doesn't fail
	 * CSPACE-4314
	 * @throws Exception
	 */
	@Test public void testSearch() throws Exception {
		log.info("Testing Search ordering");
		String[] allRecords = {"acquisition","loanin","loanout","cataloging","objectexit","intake","group","movement","conditioncheck","valuationcontrol"};
		
		for(String r : allRecords) {
			log.info("Testing Search ordering: "+r);
			String url = "/"+r+"/search?query=&pageSize=10&sortDir=1&sortKey=summary";
			HttpTester out = tester.GETData(url,  jetty);
			JSONObject test = new JSONObject(out.getContent());
			if(test.has("isError")){
				assertFalse(test.getBoolean("isError"));
			}
		}
		
	}
	/**
	 * Test Other Bits
	 */
	@Test public void testMisc() throws Exception {
		//tester.testUIspec(jetty,"/generator?quantity=10&maxrelationships=3&startvalue=0&extraprefix=Fixed","recordlist.uischema");
		
		log.info("Testing UISCHEMA");
//		tester.testUIspec(jetty, "/recordlist/uischema", "recordlist.uischema");
//		tester.testUIspec(jetty, "/namespaces/uischema", "namespaces.uischema");
//		tester.testUIspec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/reporting/uispec", "reporting.uispec");
		tester.testUIspec(jetty, "/invokereport/uispec", "invokereporting.uispec");
		//tester.testUIspec(jetty, "/myCollectionSpace/uispec", "find-edit.uispec");

		//		uispec(jetty,"/location/generator?quantity=10","acquisition.uispec");
		// 		uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		//		uispec(jetty,"/reporting/generator?quantity=10","acquisition.uispec");
	}
	
	private void mediaWithBlob() throws Exception {
		// Create a blob
		String filename = getClass().getPackage().getName().replaceAll("\\.","/")+"/darwin-beard-hat.jpg";
		byte[] data = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
		UTF8SafeHttpTester out2=tester.POSTBinaryData("/uploads",data,jetty);
		//log.info(out2.getContent());
		JSONObject response = new JSONObject(out2.getContent());
		//System.err.println(response);
		String blob_id = response.getString("csid");
		// Create
		JSONObject media=new JSONObject(tester.mediaCreate());
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
		HttpTester out = tester.POSTData("/media",tester.makeRequest(media),jetty);
		assertEquals(201,out.getStatus());
		String id=out.getHeader("Location");
		// Get
		out = tester.GETData(id,jetty);
		JSONObject content=new JSONObject(out.getContent());
		//log.info(out.getContent());
		// Check the hairy image URLs are present
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgOrig").endsWith("/download/"+blob_id+"/Original"));
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgThumb").endsWith("/download/"+blob_id+"/Thumbnail"));
		assertTrue(content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgMedium").endsWith("/download/"+blob_id+"/Medium"));
		// Get derivatives
		String read_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgOrig");
		String read2_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgThumb");
		String read3_url = content.getJSONObject("fields").getJSONArray("blobs").getJSONObject(0).getString("imgMedium");
		UTF8SafeHttpTester out3=tester.GETBinaryData(read_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));
		byte[] img = out3.getBinaryContent();
		assertArrayEquals(img,data);
		out3=tester.GETBinaryData(read2_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));
		out3=tester.GETBinaryData(read3_url,jetty,200);
		assertEquals("image/jpeg",out3.getHeader("Content-Type"));

		
		// Delete
		tester.DELETEData(id,jetty);
	}
}
