package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUIRecords extends TestBase {
	private static final Logger log=LoggerFactory.getLogger(TestUIRecords.class);
	

	/**
	 * Test Jetty
	 */
	@Test public void testJettyStartupWorks() throws Exception {
		setupJetty();
	}
	/**
	 * Test Login Status
	 */
	@Test public void testLoginStatus() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out = GETData("/loginstatus/",  jetty);
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
		ServletTester jetty=setupJetty();
		log.info("Testing CRUDL");
		testPostGetDelete(jetty, "/cataloging/", objectCreate, "distinguishingFeatures");
		log.info("Testing List");
		testLists(jetty, "cataloging",objectCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/cataloging/uispec", "collection-object.uispec");
		log.info("Testing UISCHEMA");
		testUIspec(jetty, "/cataloging/uischema", "collection-object.uischema");
	}


	/**
	 * Test Procedure CRUDL
	 */
	@Test public void testProcedureMovement() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing movement Procedure");
		testPostGetDelete(jetty, "/movement/", movementCreate, "movementReferenceNumber");
		testLists(jetty, "movement", movementCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/movement/uispec", "movement.uispec");
	}


	/**
	 * Test Intake Procedure CRUDL
	 */
	@Test public void testProcedureIntake() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing intake Procedure");
		testPostGetDelete(jetty, "/intake/", intakeCreate, "entryReason");
		testLists(jetty, "intake", intakeCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/intake/uispec", "intake.uispec");
	}


	/**
	 * Test Loanout Procedure CRUDL
	 */
	@Test public void testProcedureLoanout() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing loanout Procedure");
		testPostGetDelete(jetty, "/loanout/", loanoutCreate, "loanOutNote");
		testLists(jetty, "loanout", loanoutCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/loanout/uispec", "loanout.uispec");
	}


	/**
	 * Test Loanin Procedure CRUDL
	 */
	@Test public void testProcedureLoanin() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing loanin Procedure");
		testPostGetDelete(jetty, "/loanin/", loaninCreate, "loanInNote");
		testLists(jetty, "loanin", loaninCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/loanin/uispec", "loanin.uispec");
	}


	/**
	 * Test Acquisition Procedure CRUDL
	 */
	@Test public void testProcedureAcquisition() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing acquisition Procedure");
		testPostGetDelete(jetty, "/acquisition/", acquisitionCreate, "acquisitionReason");
		testLists(jetty, "acquisition", acquisitionCreate, "items");
		log.info("Testing UISCHEMA");
		testUIspec(jetty, "/acquisition/uischema", "acquisition.uischema");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/acquisition/uispec", "acquisition.uispec");
	}


	/**
	 * Test Group Procedure CRUDL
	 */
	@Test public void testProcedureGroup() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing group Procedure");
		testPostGetDelete(jetty, "/group/", groupCreate, "title");
		testLists(jetty, "group", groupCreate, "items");

		log.info("Testing UISPEC");
		testUIspec(jetty, "/group/uispec", "group.uispec");
	}
	/**
	 * Test ObjectExit Procedure CRUDL
	 */
	@Test public void testProcedureObjectexit() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing objectexit Procedure");
		testPostGetDelete(jetty, "/objectexit/", objectexitCreate, "exitNumber");
		testLists(jetty, "objectexit", objectexitCreate, "items");

		log.info("Testing UISPEC");
		testUIspec(jetty, "/objectexit/uispec", "objectexit.uispec");
		//objectexit
	}
	
	/**
	 * Test Authorities
	 */
	@Test public void testAuthorities() throws Exception {

		ServletTester jetty=setupJetty();
		log.info("Testing UISPEC");
		testUIspec(jetty, "/person/uispec", "person.uispec");
		testUIspec(jetty, "/location/uispec", "location.uispec");
//		uispec(jetty, "/organization/uispec", "organization-authority.uispec");
		
	}
	

	/**
	 * Test Media CRUDL
	 */
	@Test public void testProcedureMedia() throws Exception {
		ServletTester jetty=setupJetty();
		testPostGetDelete(jetty, "/media/", mediaCreate, "identificationNumber");
		testLists(jetty, "media", mediaCreate, "items");
		log.info("Testing UISPEC");
		testUIspec(jetty, "/media/uispec", "media.uispec");

		log.info("Testing UISPEC for content");
		HttpTester out = GETData("/media/uispec",jetty);
		assertEquals(200,out.getStatus());
		JSONObject spec=new JSONObject(out.getContent());
		assertEquals("${fields.blobs.0.length}",spec.getJSONObject("recordEditor").getString(".csc-blobs-length"));
	}
	
	/**
	 * Test Media Blob CRUDL
	 */
	@Test public void testProcedureMediaBlob() throws Exception {
		ServletTester jetty=setupJetty();
		mediaWithBlob();
	}
	
	/**
	 * Test Vocabulary / TermList CRUDL
	 */
	@Test public void testTermLists() throws Exception {
		ServletTester jetty=setupJetty();
		log.info("Testing UISPEC");
		testUIspec(jetty, "/termlist/uispec", "termlist.uispec");

	}

	/**
	 * Test Other Bits
	 */
	@Test public void testMisc() throws Exception {
		ServletTester jetty=setupJetty();
		
		log.info("Testing UISCHEMA");
		testUIspec(jetty, "/recordlist/uischema", "recordlist.uischema");
		testUIspec(jetty, "/namespaces/uischema", "namespaces.uischema");
		testUIspec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
		log.info("Testing UISPEC");
		testUIspec(jetty, "/reporting/uispec", "reporting.uispec");
		testUIspec(jetty, "/invokereport/uispec", "invokereporting.uispec");
		testUIspec(jetty, "/myCollectionSpace/uispec", "find-edit.uispec");

		//		uispec(jetty,"/location/generator?quantity=10","acquisition.uispec");
		// 		uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		//		uispec(jetty,"/reporting/generator?quantity=10","acquisition.uispec");
	}
	
	/**
	 * 
	 */

	
	
	
	
	
	
	private void mediaWithBlob() throws Exception {
		ServletTester jetty = setupJetty();
		// Create a blob
		String filename = getClass().getPackage().getName().replaceAll("\\.","/")+"/darwin-beard-hat.jpg";
		byte[] data = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
		UTF8SafeHttpTester out2=POSTBinaryData("/uploads",data,jetty);
		//log.info(out2.getContent());
		JSONObject response = new JSONObject(out2.getContent());
		//System.err.println(response);
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
		//log.info(out.getContent());
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
}
