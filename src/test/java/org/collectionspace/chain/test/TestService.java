package org.collectionspace.chain.test;

import org.collectionspace.chain.services.ReturnedDocument;
import org.collectionspace.chain.services.ServicesConnection;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestService {
	private static String BASE_URL="http://localhost:8080";
		
	@Before public void checkServicesRunning() throws BadRequestException {
		try {
			ServicesConnection conn=new ServicesConnection(BASE_URL+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
		} catch(BadRequestException e) {
			Assume.assumeTrue(false);
		}
	}
	
	@Test public void testAssumptionMechanism() {
		System.err.println("Services Running!");
	}

	@Test public void testObjectsPost() {
		
	}
}
