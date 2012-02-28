package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.persistence.TestBase;
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
	 * Test Authorities
	 */
	@Test public void testAuthorities() throws Exception {
		log.info("Testing UISPEC");
		tester.testUIspec(jetty, "/person/uischema", "person.uischema");
		tester.testUIspec(jetty, "/person/uispec", "person.uispec");
		tester.testUIspec(jetty, "/location/uispec", "location.uispec");
		tester.testUIspec(jetty, "/organization/uispec", "organization-authority.uispec");
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
	 * Test Other Bits
	 */
	@Test public void testMisc() throws Exception {
		//tester.testUIspec(jetty,"/generator?quantity=10&maxrelationships=3&startvalue=0&extraprefix=Fixed","recordlist.uischema");
		
		log.info("Testing UISCHEMA");
		tester.testUIspec(jetty, "/recordlist/uischema", "recordlist.uischema");
		tester.testUIspec(jetty, "/namespaces/uischema", "namespaces.uischema");
		tester.testUIspec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
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
