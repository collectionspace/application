package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.ServicesConnection;
import org.collectionspace.chain.csp.persistence.services.ServicesStorage;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.chain.util.json.JSONUtils;
import org.dom4j.Node;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestServiceThroughAPI extends ServicesBaseClass {
	// XXX refactor
	private JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		assertNotNull(stream);
		String data=IOUtils.toString(stream);
		stream.close();		
		return new JSONObject(data);
	}

	// XXX refactor
	@SuppressWarnings("unchecked")
	private void deleteAll() throws Exception {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/");
		if(all.getStatus()!=200)
			throw new ConnectionException("Bad request during identifier cache map update: status not 200");
		List<Node> objects=all.getDocument().selectNodes("collection-object-list/collection-object-list-item");
		for(Node object : objects) {
			String csid=object.selectSingleNode("csid").getText();
			conn.getNone(RequestMethod.DELETE,"collectionobjects/"+csid,null);
		}
	}
	
	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsPut() throws Exception {
		deleteAll();
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		ss.createJSON("collection-object/testObjectsPut",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/testObjectsPut");
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
	}

	// XXX use autocreate not create when create dies
	@Test public void testObjectsPost() throws Exception {
		deleteAll();
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		ss.createJSON("collection-object/testObjectsPost",getJSON("obj3.json"));
		ss.updateJSON("collection-object/testObjectsPost",getJSON("obj4.json"));
		JSONObject js=ss.retrieveJSON("collection-object/testObjectsPost");
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj4.json")));
	}

	// XXX use autocreate not create when create dies
	@Test public void testObjectsDelete() throws Exception {
		deleteAll();
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		ss.createJSON("collection-object/testObjectsDelete",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/testObjectsDelete");
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
		ss.deleteJSON("collection-object/testObjectsDelete");
		try {
			ss.retrieveJSON("collection-object/testObjectsDelete");
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
	
	@Test public void testGetId() throws Exception {
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		JSONObject jo=ss.retrieveJSON("id/intake");
		assertTrue(jo.getString("next").startsWith("IN2009."));
		jo=ss.retrieveJSON("id/objects");
		System.err.println(jo);
		assertTrue(jo.getString("next").startsWith("2009.1."));
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsList() throws Exception {
		deleteAll();
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		ss.createJSON("collection-object/name1",getJSON("obj3.json"));
		ss.createJSON("collection-object/name2",getJSON("obj4.json"));
		ss.createJSON("collection-object/123",getJSON("obj4.json"));
		String[] names=ss.getPaths("collection-object");
		assertArrayContainsString(names,"name1");
		assertArrayContainsString(names,"name2");
		assertArrayContainsString(names,"123");
	}
	
	@Test public void testHackCSPACE264() throws Exception {
		deleteAll();
		ServicesStorage ss=new ServicesStorage(base+"/cspace-services/");
		ss.createJSON("collection-object/def",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/def");
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
	}
}
