package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
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
	private static Pattern vocab_urn=Pattern.compile("urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'");
		
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}
	
	@Test public void testAuthorities() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		testAllAuthorities(ss,"/person/person","displayName");
		testAllAuthorities(ss,"/vocab/xxx","displayName");
		testAllAuthorities(ss,"/organization/organization","displayName");
		//testAllAuthorities(ss,"/place/place","displayName");
	}
	
	private void testAllAuthorities(Storage ss, String path, String testField) throws Exception {
		// Create
		JSONObject data=new JSONObject();
		data.put("shortIdentifier","TEST");
		data.put(testField,"TEST");

		data.put("termStatus","Provisional");
		String id=ss.autocreateJSON(path,data);
		// Read
		JSONObject out=ss.retrieveJSON(path+"/"+id);
		assertEquals("TEST",out.getString(testField));
		assertEquals("Provisional",out.getString("termStatus"));
		// Update
		data.remove(testField);
		data.put(testField,"TEST2");
		data.put("termStatus","Provisional2");
		ss.updateJSON(path + "/"+id,data);
		out=ss.retrieveJSON(path + "/"+id);
		assertEquals("TEST2",out.getString(testField));
		assertEquals("Provisional2",out.getString("termStatus"));
		String id3=out.getString("csid");
		// List
		data.remove(testField);
		data.put(testField,"TEST3");
		String id2=ss.autocreateJSON(path,data);
		out=ss.retrieveJSON(path + "/"+id2);
		assertEquals("TEST3",out.getString(testField));		
		boolean found1=false,found2=false;
		JSONObject myjs = new JSONObject();
		myjs.put("pageSize", "100");
		myjs.put("pageNum", "0");
		int resultsize=1;
		int check = 0;
		String checkpagination = "";
		while(resultsize >0){
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
		ss.deleteJSON(path + "/" + id2);
		ss.deleteJSON(path + "/" + id3);
		try {
			out=ss.retrieveJSON(path + "/" + id2);
			out=ss.retrieveJSON(path + "/" + id3);		
			assertTrue(false);
		} catch(ExistException x) {
			assertTrue(true);
		}
	}
	
	

	
}
