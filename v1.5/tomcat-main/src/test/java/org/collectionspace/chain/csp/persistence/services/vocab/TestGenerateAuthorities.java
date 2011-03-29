package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGenerateAuthorities extends TestBase {
	private static final Logger log = LoggerFactory
	.getLogger(TestGenerateAuthorities.class);
	
	
	@Test
	public void testSetUp() throws Exception {
		HttpTester out;
		log.info("initialize authorities");

		ServletTester jetty = setupJetty();
		
		// repopulate the authorities with dummy data
		out = GETData("/reset/nodelete", jetty);
		

		//do we have any records
		out = GETData("/authorities/organization/?pageSize=1", jetty);
		JSONArray results = new JSONObject(out.getContent()).getJSONArray("items");
		assertTrue(results.length() > 0);
		

		//do we have any records
		out = GETData("/authorities/person/?pageSize=1", jetty);
		JSONArray results2 = new JSONObject(out.getContent()).getJSONArray("items");
		assertTrue(results2.length() > 0);
		
		// update and remove fields not in each list within an authority
		// /chain/vocabularies/person/initialize?datapath=/Users/csm22/Documents/collectionspace/svcapp/cspi-webui/src/main/resources/org/collectionspace/chain/csp/webui/misc/names.txt

	}
}
