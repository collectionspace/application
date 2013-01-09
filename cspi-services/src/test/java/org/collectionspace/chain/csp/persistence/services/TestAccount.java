/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAccount extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestAccount.class);
	
	@Before public void checkServicesRunning() throws ConnectionException {
		setup();
	}
	
	//XXX this test needs work
	//@Test 
	public void testAccountSearch() {
		
		Storage ss;
		try {
			ss = makeServicesStorage();
			/* 
			 *  arggg how do I get it to do an exact match */
			JSONObject data = ss.getPathsJSON("users/",new JSONObject("{\"email\":\"bob@indigo-e.co.uk\"}"));
			String[] paths=(String[])data.get("listItems");
	
			
			if(paths.length>=1){
				for(int i=0;i<paths.length;i++) {
					//log.info(paths[i] +"  : "+ i +" of "+ paths.length);
				}
			}
		} catch (CSPDependencyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnimplementedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnderlyingStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Test public void testAccountCreate() throws Exception {
		Storage ss=makeServicesStorage();
		/* delete user so we can create it later - will return 404 if user doesn't exist */
		JSONObject data = ss.getPathsJSON("users/",new JSONObject("{\"userId\":\"test31@collectionspace.org\"}"));
		String[] paths= (String[])data.get("listItems");
		if(paths.length>0)
			ss.deleteJSON("users/"+paths[0]);
		
		JSONObject u1=getJSON("user1.json");
		/* create the user based on json */
		/* will give a hidden 500 error if userid is not unique (useful eh?) */
		String path=ss.autocreateJSON("users/",u1,null);
		assertNotNull(path);
		JSONObject u2=getJSON("user1.json");
		ss.updateJSON("users/"+path,u2, new JSONObject());
		JSONObject u3=ss.retrieveJSON("users/"+path, new JSONObject());
		assertNotNull(u3);
		// Check output
		assertEquals("Test Mccollectionspace.org",u3.getString("screenName"));
		assertEquals("test31@collectionspace.org",u3.getString("userId"));
		assertEquals("test31@collectionspace.org",u3.getString("email"));
		assertEquals("active",u3.getString("status"));
		// Check the method we're about to use to check if login works works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test31@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"blahblah");
		cache.reset();
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		assertFalse(out.getStatus()==200);		
		// Check login works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test31@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"testtestt");
		cache.reset();
		out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		log.debug("Status",out.getStatus());
		//assertTrue(out.getStatus()==200);
		//
		ss.deleteJSON("users/"+path);
		/* tidy up and delete user */
	}
	

}
