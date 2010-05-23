package org.collectionspace.chain.csp.persistence.services.vocab;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.ServicesBaseClass;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.RequestCache;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class TestVocab extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestVocab.class);
	private static Pattern vocab_urn=Pattern.compile("urn:cspace:org.collectionspace.demo:vocabulary\\((.*?)\\):item\\((.*?)\\)'(.*?)'");
		
	private InputStream getRootSource(String file) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}
	
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}
	
	@Test public void testVocab() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		// Create
		JSONObject data=new JSONObject();
		data.put("name","TEST");
		String id=ss.autocreateJSON("/vocab/xxx",data);
		Matcher m=vocab_urn.matcher(id);
		assertTrue(m.matches());
		assertEquals("TEST",m.group(3));
		// Read
		JSONObject out=ss.retrieveJSON("/vocab/xxx/"+id);
		assertEquals("TEST",out.getString("name"));
		// Update
		data.remove("name");
		data.put("name","TEST2");
		ss.updateJSON("/vocab/xxx/"+id,data);
		out=ss.retrieveJSON("/vocab/xxx/"+id);
		assertEquals("TEST2",out.getString("name"));
		String id3=out.getString("csid");
		// List
		data.remove("name");
		data.put("name","TEST3");
		String id2=ss.autocreateJSON("/vocab/xxx",data);
		out=ss.retrieveJSON("/vocab/xxx/"+id2);
		assertEquals("TEST3",out.getString("name"));
		
		boolean found1=false,found2=false;
		JSONObject myjs = new JSONObject();
		myjs.put("pageSize", "100");
		myjs.put("pageNum", "1");
		int resultsize=1;
		int check = 0;
		String checkpagination = "";
		while(resultsize >0){
			myjs.put("pageNum", check);
			check++;
			String[] res = ss.getPaths("/vocab/xxx",myjs);

			resultsize=res.length;
			if(res.length==0 || checkpagination.equals(res[0])){
				resultsize=0;
				//testing whether we have actually returned the same page or the next page - all csid returned should be unique
			}
			else{
				checkpagination = res[0];
			}
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
		log.debug("id2="+id2+" f="+found2);
		log.debug("id3="+id3+" f="+found1);
		assertTrue(found1);
		assertTrue(found2);
		// Delete
		ss.deleteJSON("/vocab/xxx/"+id2);
		ss.deleteJSON("/vocab/xxx/"+id3);
		try {
			out=ss.retrieveJSON("/vocab/xxx/"+id2);
			out=ss.retrieveJSON("/vocab/xxx/"+id3);		
			assertTrue(false);
		} catch(ExistException x) {}
	}

	@Test public void testName() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		// Create
		JSONObject data=new JSONObject();
		data.put("displayName","TEST");
		//XXX why has staus disappeared?
		//data.put("status","Provisional");
		String id=ss.autocreateJSON("/person/person",data);
		// Read
		JSONObject out=ss.retrieveJSON("/person/person/"+id);
		log.info(out.toString());
		assertEquals("TEST",out.getString("displayName"));
		//assertEquals("Provisional",out.getString("status"));
		// Update
		data.remove("displayName");
		data.put("displayName","TEST2");
		//data.put("status","Provisional2");
		ss.updateJSON("/person/person/"+id,data);
		out=ss.retrieveJSON("/person/person/"+id);
		assertEquals("TEST2",out.getString("displayName"));
		//assertEquals("Provisional2",out.getString("status"));
		String id3=out.getString("csid");
		// List
		data.remove("displayName");
		data.put("displayName","TEST3");
		String id2=ss.autocreateJSON("/person/person",data);
		out=ss.retrieveJSON("/person/person/"+id2);
		assertEquals("TEST3",out.getString("displayName"));		
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
			String[] res = ss.getPaths("/person/person",myjs);

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
		ss.deleteJSON("/person/person/"+id2);
		ss.deleteJSON("/person/person/"+id3);
		try {
			out=ss.retrieveJSON("/person/person/"+id2);
			out=ss.retrieveJSON("/person/person/"+id3);		
			assertTrue(false);
		} catch(ExistException x) {}
	}
	
	
	/* XXX implement once placeauthority is sorted at the service layer 
	@Test public void testPlace() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		// Create
		JSONObject data=new JSONObject();
		data.put("displayName","TEST");
		data.put("status","Provisional");
		String id=ss.autocreateJSON("/place/place",data);
		// Read
		JSONObject out=ss.retrieveJSON("/place/place/"+id);
		assertEquals("TEST",out.getString("displayName"));
		assertEquals("Provisional",out.getString("status"));
		// Update
		data.remove("displayName");
		data.put("displayName","TEST2");
		data.put("status","Provisional2");
		ss.updateJSON("/place/place/"+id,data);
		out=ss.retrieveJSON("/place/place/"+id);
		assertEquals("TEST2",out.getString("displayName"));
		assertEquals("Provisional2",out.getString("status"));
		String id3=out.getString("csid");
		// List
		data.remove("displayName");
		data.put("displayName","TEST3");
		String id2=ss.autocreateJSON("/place/place",data);
		out=ss.retrieveJSON("/place/place/"+id2);
		assertEquals("TEST3",out.getString("displayName"));
		boolean found1=false,found2=false;
		JSONObject myjs = new JSONObject();
		myjs.put("pageSize", "100");
		myjs.put("pageNum", "1");
		int resultsize=1;
		int check = 0;
		String checkpagination = "";
		while(resultsize >0){
			myjs.put("pageNum", check);
			check++;
			String[] res = ss.getPaths("/place/place",myjs);

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
		log.info("id2="+id2+" f="+found2);
		log.info("id3="+id3+" f="+found1);
		// XXX pagination: failing because pagination support is not there yet 
		//assertTrue(found1);
		//assertTrue(found2);
		// Delete
		ss.deleteJSON("/place/place/"+id2);
		ss.deleteJSON("/place/place/"+id3);
		try {
			out=ss.retrieveJSON("/place/place/"+id2);
			out=ss.retrieveJSON("/place/place/"+id3);		
			assertTrue(false);
		} catch(ExistException x) {}
	}
	
*/

	// XXX factor tests
	@Test public void testOrgs() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		// Create
		JSONObject data=new JSONObject();
		data.put("displayName","TEST");
		String id=ss.autocreateJSON("/organization/organization",data);
		// Read
		JSONObject out=ss.retrieveJSON("/organization/organization/"+id);
		assertEquals("TEST",out.getString("displayName"));
		// Update
		data.remove("name");
		data.put("displayName","TEST2");
		ss.updateJSON("/organization/organization/"+id,data);
		out=ss.retrieveJSON("/organization/organization/"+id);
		assertEquals("TEST2",out.getString("displayName"));
		String id3=out.getString("csid");
		// List
		data.remove("name");
		data.put("displayName","TEST3");
		String id2=ss.autocreateJSON("/organization/organization",data);
		out=ss.retrieveJSON("/organization/organization/"+id2);
		assertEquals("TEST3",out.getString("displayName"));		

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
			String[] res = ss.getPaths("/organization/organization",myjs);

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
		
		log.info("id2="+id2+" f="+found2);
		log.info("id3="+id3+" f="+found1);
		assertTrue(found1);
		assertTrue(found2);
//		// Delete
		ss.deleteJSON("/organization/organization/"+id2);
		ss.deleteJSON("/organization/organization/"+id3);
		try {
			out=ss.retrieveJSON("/organization/organization/"+id2);
			out=ss.retrieveJSON("/organization/organization/"+id3);		
			assertTrue(false);
		} catch(ExistException x) {}
	}
	
// if you want to delete all orgs you can run this one @Test //
	public void testDelAllOrgs() throws Exception {

		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject myjs = new JSONObject();
		myjs.put("pageSize", "100");
		myjs.put("pageNum", "0");
		int resultsize=1;
		int check = 0;
		String checkpagination = "";

		while(resultsize >0){
			myjs.put("pageNum", check);
			String[] res = ss.getPaths("/organization/organization",myjs);

			if(res.length==0 || checkpagination.equals(res[0])){
				resultsize=0;
				//testing whether we have actually returned the same page or the next page - all csid returned should be unique
			}
			else{
				checkpagination = res[0];
			}
			resultsize=res.length;
			for(String urn : res) {
				try {
					ss.deleteJSON("/organization/organization/"+urn);
					log.info(check + "  Deleting "+urn);
				} catch(Exception e) { /* Sometimes records are wdged */ }
				
			}
		}
	}
	
}
