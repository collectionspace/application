package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
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
		// String vocabtype="loanoutstatus";
		ServletTester jetty = setupJetty();
		// delete all records, procedures,vocabularies created and repopulate the authorities with dummy data
		out = GETData("/reset/nodelete", jetty);

		// update and remove fields not in each list within an authority
		// /chain/vocabularies/person/initialize?datapath=/Users/csm22/Documents/collectionspace/svcapp/cspi-webui/src/main/resources/org/collectionspace/chain/csp/webui/misc/names.txt

	}
}
