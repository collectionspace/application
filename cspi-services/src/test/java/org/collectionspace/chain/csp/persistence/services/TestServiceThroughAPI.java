package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.util.List;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.dom4j.Node;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceThroughAPI extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestServiceThroughAPI.class);

	// XXX refactor
	@SuppressWarnings("unchecked")
	private void deleteAll() throws Exception {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/",null,creds,cache);
		if(all.getStatus()!=200)
			throw new ConnectionException("Bad request during identifier cache map update: status not 200");
		List<Node> objects=all.getDocument().selectNodes("collectionobjects-common-list/collection-object-list-item");
		for(Node object : objects) {
			String csid=object.selectSingleNode("csid").getText();
			conn.getNone(RequestMethod.DELETE,"collectionobjects/"+csid,null,creds,cache);
		}
	}
	
	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
			
	@Test public void testObjectsPut() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertEquals(js.get("title"),getJSON("obj3.json").get("title"));
	}
	
	@Test public void testObjectsPost() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		ss.updateJSON("collection-object/"+path,getJSON("obj4.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertEquals(js.get("title"),getJSON("obj4.json").get("title"));
	}

	@Test public void testObjectsDelete() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertEquals(js.get("title"),getJSON("obj3.json").get("title"));
		ss.deleteJSON("collection-object/"+path);
		try {
			ss.retrieveJSON("collection-object/"+path);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
	}
	
	// XXX factor out
	private static void assertArrayContainsString(String[] a,String b) {
		for(String x: a) {
			if(x.equals(b))
				return;
		}
		assertFalse(true);
	}

	// XXX factor out
	private static void assertArrayDoesNotContainString(String[] a,String b) {
		for(String x: a) {
			if(x.equals(b))
				assertFalse(true);
		}
	}
	
	@Test public void testGetId() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject jo=ss.retrieveJSON("id/intake");
		assertTrue(jo.getString("next").startsWith("IN2010."));
		jo=ss.retrieveJSON("id/objects");
		//log.info("JSON",jo);
		assertTrue(jo.getString("next").startsWith("2010.1."));
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsList() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String p3=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		JSONObject data = ss.getPathsJSON("collection-object",null);
		String[] names=(String[]) data.get("listItems");
		//XXX add pagination support CSPACE-1836
		assertArrayContainsString(names,p1);
		assertArrayContainsString(names,p2);
		assertArrayContainsString(names,p3);

		ss.deleteJSON("collection-object/"+p1);
		try {
			ss.retrieveJSON("collection-object/"+p1);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
		ss.deleteJSON("collection-object/"+p2);
		try {
			ss.retrieveJSON("collection-object/"+p2);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
		ss.deleteJSON("collection-object/"+p3);
		try {
			ss.retrieveJSON("collection-object/"+p3);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
	}
	
	@Test public void testSearch() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj-search.json"));
		JSONObject restriction=new JSONObject();
		restriction.put("keywords","aardvark");
		JSONObject data = ss.getPathsJSON("collection-object",restriction);
		String[] names= (String[])data.get("listItems");
		//XXX add pagination support CSPACE-1836
		assertArrayContainsString(names,p2);
		assertArrayDoesNotContainString(names,p1);
		ss.deleteJSON("collection-object/"+p1);
		try {
			ss.retrieveJSON("collection-object/"+p1);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
		ss.deleteJSON("collection-object/"+p2);
		try {
			ss.retrieveJSON("collection-object/"+p2);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
	}
	
	@Test public void testMini() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("intake/",getJSON("int4.json"));
		JSONObject mini=ss.retrieveJSON("intake/"+p1+"/view");
		assertEquals("depositorX",mini.getString("summary"));
		assertEquals("entryNumberX",mini.getString("number"));	
		ss.deleteJSON("intake/"+p1);
		try {
			ss.retrieveJSON("intake/"+p1);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}	
	}

	private JSONObject makePerson(String pname) throws Exception {
		JSONObject out=new JSONObject();
		out.put("displayName","Dic Penderyn");
		if(pname!=null)
			out.put("group",pname);
		return out;
	}
	
	@Test public void testAuthorityRefs() throws Exception {
		// Create a record with references
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject person=makePerson(null);
		String p=ss.autocreateJSON("person/person",person);
		log.info("p="+p);
		JSONObject po=ss.retrieveJSON("person/person/"+p);
		log.info("po="+po);
		String pname=po.getString("refid");
		//
		JSONObject person2=makePerson(pname);
		String p2=ss.autocreateJSON("person/person",person2);
		log.info("p2="+p2);
		//
		JSONObject data=getJSON("int4.json");
		data.remove("valuer");
		data.put("valuer",pname);
		String p1=ss.autocreateJSON("intake/",data);
		log.info("p1="+p1);
		JSONObject mini=ss.retrieveJSON("intake/"+p1+"/refs");
		log.info("mini="+mini);
		JSONObject member=mini.getJSONObject("intakes_common:valuer");		
		assertNotNull(member);
		assertEquals("Dic Penderyn",member.getString("displayName"));
		

		ss.deleteJSON("person/person/"+p);
		try {
			ss.retrieveJSON("person/person/"+p);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}
		
		ss.deleteJSON("person/person/"+p2);
		try {
			ss.retrieveJSON("person/person/"+p2);
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(ExistException e) {}	
		// XXX retrieve by authority
		// XXX also authorities
	}
}
