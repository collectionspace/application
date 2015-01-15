package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGenerateAuthorities {
	private static final Logger log = LoggerFactory
	.getLogger(TestGenerateAuthorities.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			log.error("TestGenerateAuthorities: Could not set up jetty! " + ex.getLocalizedMessage());
		}
	}
	
	@AfterClass public static  void testStop() throws Exception {
		tester.stopJetty(jetty);
	}
	

	
	@Test
	public void testSetUp() throws Exception {
		HttpTester out;
		log.info("initialize authorities");

		//String urltest = "/vocabularies/currency/refresh?datapath=/Users/csm22/Documents/collectionspace/test.txt";
		//out = tester.GETData(urltest, jetty);

		
		// repopulate the authorities with dummy data
		//DONOT RUN THIS TEST LOCALLY 
		out = tester.GETData("/reset/keepVocabs/", jetty); // passing in 'keepVocabs' on the path keeps the default term lists from being reset
		

		//do we have any records
		out = tester.GETData("/authorities/organization/?pageSize=1", jetty);
		JSONArray resultsOrg = new JSONObject(out.getContent()).getJSONArray("items");
		assertTrue(resultsOrg.length() > 0);

		//do we have any records
		out = tester.GETData("/authorities/person/?pageSize=1", jetty);
		JSONArray resultsPerson = new JSONObject(out.getContent()).getJSONArray("items");
		assertTrue(resultsPerson.length() > 0);
		
		//make sure all the vocabs are initialized
		out = tester.GETData(TestBase.AUTHS_INIT_PATH, jetty);
		// update and remove fields not in each list within an authority
		// /chain/vocabularies/person/initialize?datapath=/Users/csm22/Documents/collectionspace/svcapp/cspi-webui/src/main/resources/org/collectionspace/chain/csp/webui/misc/names.txt

	}
	
	
	
}
