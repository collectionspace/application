/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;


import org.collectionspace.chain.csp.persistence.services.ServicesBaseClass;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVocab extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestVocab.class);
		
	@Before public void checkServicesRunning() throws ConnectionException {
		setup();
	}
	
	@Test public void testAuthorities() throws Exception {
		log.info("testAuthorities_start");
		Storage ss=makeServicesStorage();
		log.info("testAuthorities:person");

		//XXX disable until soft delete works better everywhere
		//testAllAuthorities(ss,"/person/defaultPersonAuthority","displayName");
		log.info("testAuthorities:vocab");
		//testAllAuthorities(ss,"/vocab/xxx","displayName");
		log.info("testAuthorities:organization");
		//XXX disable until soft delete works better everywhere
		//testAllAuthorities(ss,"/organization/organization","displayName");
		//testAllAuthorities(ss,"/place/place","displayName");
		log.info("testAuthorities:work");
		//testAllAuthorities(ss,"/work/work","displayName");
		log.info("testAuthorities:concept");
		//testAllAuthorities(ss,"/concept/concept","displayName");
		//testAllAuthorities(ss,"/citation/citation","displayName");
		log.info("testAuthorities_finish");
	}
	
	private void testAllAuthorities(Storage ss, String path, String testField) throws Exception {
		// Create
		log.info("testAuthorities_"+path+"_create");
		JSONObject data=new JSONObject();
		data.put("shortIdentifier","TEST3");
		data.put(testField,"TEST3");

		data.put("termStatus","Provisional");
		String id=ss.autocreateJSON(path,data,null);
		// Read
		log.info("testAuthorities_"+path+"_read");
		JSONObject out=ss.retrieveJSON(path+"/"+id, new JSONObject());
		assertEquals("TEST3",out.getString(testField));
		assertEquals("Provisional",out.getString("termStatus"));
		// Update
		log.info("testAuthorities_"+path+"_update");
		data.remove(testField);
		data.put(testField,"TEST2");
		data.put("termStatus","Provisional2");
		ss.updateJSON(path + "/"+id,data, new JSONObject());
		out=ss.retrieveJSON(path + "/"+id, new JSONObject());
		assertEquals("TEST2",out.getString(testField));
		assertEquals("Provisional2",out.getString("termStatus"));
		String id3=out.getString("csid");
		// List
		log.info("testAuthorities_"+path+"_list");
		data.remove(testField);
		data.put(testField,"TEST4");
		data.put("shortIdentifier","TEST4");
		String id2=ss.autocreateJSON(path,data,null);
		out=ss.retrieveJSON(path + "/"+id2, new JSONObject());
		assertEquals("TEST4",out.getString(testField));		
		boolean found1=false,found2=false;
		JSONObject myjs = new JSONObject();
		myjs.put("pageSize", "100");
		myjs.put("pageNum", "0");
		int resultsize=1;
		int check = 0;
		String checkpagination = "";
		while(resultsize >0){
			log.info("testAuthorities_"+path+"_page: "+check);
			myjs.put("pageNum", check);
			check++;
			JSONObject items = ss.getPathsJSON(path,myjs);

			String[] res = (String[])items.get("listItems");

			if(res.length==0 || checkpagination.equals(res[0])){
				resultsize=0;
				//testing whether we have actually returned the same page or the next page - all csid returned should be unique
			}
			else{
				checkpagination = res[0];
			}
			resultsize=res.length;
			for(String u : res) {
				if(id3.equals(u)){
					found1=true;
				}
				if(id2.equals(u)){
					found2=true;
				}
			}
			if(found1 && found2){
				resultsize=0;
			}
		}
		assertTrue(found1);
		assertTrue(found2);
		// Delete
		log.info("testAuthorities_"+path+"_delete");
		ss.deleteJSON(path + "/" + id2);
		ss.deleteJSON(path + "/" + id3);
		try {
			out=ss.retrieveJSON(path + "/" + id2, new JSONObject());
			out=ss.retrieveJSON(path + "/" + id3, new JSONObject());		
			assertTrue(false);
		} catch(ExistException x) {
			assertTrue(true);
		}
	}
	
	

	
}
