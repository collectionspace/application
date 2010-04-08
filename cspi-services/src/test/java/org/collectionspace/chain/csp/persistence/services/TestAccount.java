package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.csp.api.persistence.Storage;
import org.json.JSONObject;
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
		
		
	}
}
