package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.ServicesStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.core.RequestCache;
import org.collectionspace.chain.util.json.JSONUtils;
import org.dom4j.Document;
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
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/",null);
		if(all.getStatus()!=200)
			throw new ConnectionException("Bad request during identifier cache map update: status not 200");
		List<Node> objects=all.getDocument().selectNodes("collectionobjects-common-list/collection-object-list-item");
		for(Node object : objects) {
			String csid=object.selectSingleNode("csid").getText();
			conn.getNone(RequestMethod.DELETE,"collectionobjects/"+csid,null);
		}
	}
	
	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
	
	private static Storage makeServicesStorage(String path) throws CSPDependencyException {
		return new ServicesStorageGenerator(path).getStorage(new RequestCache());
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsPut() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		System.err.println("path="+path);
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
	}

	// XXX use autocreate not create when create dies
	@Test public void testObjectsPost() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		ss.updateJSON("collection-object/"+path,getJSON("obj4.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj4.json")));
	}

	// XXX use autocreate not create when create dies
	@Test public void testObjectsDelete() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
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
	
	@Test public void testGetId() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject jo=ss.retrieveJSON("id/intake");
		assertTrue(jo.getString("next").startsWith("IN2009."));
		jo=ss.retrieveJSON("id/objects");
		System.err.println(jo);
		assertTrue(jo.getString("next").startsWith("2009.1."));
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsList() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String p3=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String[] names=ss.getPaths("collection-object");
		assertArrayContainsString(names,p1);
		assertArrayContainsString(names,p2);
		assertArrayContainsString(names,p3);
	}
}
