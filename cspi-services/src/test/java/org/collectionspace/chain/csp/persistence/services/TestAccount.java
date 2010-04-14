package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.csp.api.persistence.Storage;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestAccount extends ServicesBaseClass {

	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
	
	@Test public void testAccountCreate() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String[] paths=ss.getPaths("users/",new JSONObject("{\"userId\":\"test@example.org\"}"));
		if(paths.length>0)
			ss.deleteJSON("users/"+paths[0]);
		JSONObject u1=getJSON("user1.json");
		String path=ss.autocreateJSON("users/",u1);
		System.err.println("path="+path);
		assertNotNull(path);
		JSONObject u2=getJSON("user1.json");
		ss.updateJSON("users/"+path,u2);
		JSONObject u3=ss.retrieveJSON("users/"+path);
		assertNotNull(u3);
		System.err.println(u3);
		// Check output
		assertEquals("Test McTest",u3.getString("screenName"));
		assertEquals("test@example.org",u3.getString("userId"));
		assertEquals("test@example.org",u3.getString("email"));
		assertEquals("active",u3.getString("status"));
		// Check the method we're about to use to check if login works works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@example.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"blahblah");
		cache.reset();
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		assertFalse(out.getStatus()==200);		
		// Check login works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@example.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"testtestt");
		cache.reset();
		out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		assertTrue(out.getStatus()==200);
		//
		ss.deleteJSON("users/"+path);
	}
}
