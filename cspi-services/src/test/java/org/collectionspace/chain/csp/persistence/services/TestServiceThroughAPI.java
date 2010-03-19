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
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.ServicesStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.RequestCache;
import org.collectionspace.chain.util.json.JSONUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class TestServiceThroughAPI extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestServiceThroughAPI.class);
	// XXX refactor
	private JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		log.info(path);
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
	
	private InputStream getRootSource(String file) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}
	
	private Storage makeServicesStorage(String path) throws CSPDependencyException {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		cspm.configure(new InputSource(getRootSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		log.info(spec.dump());
		Record r_obj=spec.getRecord("collection-object");
		assertNotNull(r_obj);
		assertEquals("collection-object",r_obj.getID());
		assertEquals("objects",r_obj.getWebURL());
		return cspm.getStorage("service").getStorage(new RequestCache());
	}
	
	@Test public void testObjectsPut() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		log.info("path="+path);
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj3.json")));
	}
	
	@Test public void testObjectsPost() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String path=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		ss.updateJSON("collection-object/"+path,getJSON("obj4.json"));
		JSONObject js=ss.retrieveJSON("collection-object/"+path);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(js,getJSON("obj4.json")));
	}

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
		log.info("JSON",jo);
		assertTrue(jo.getString("next").startsWith("2010.1."));
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsList() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String p3=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String[] names=ss.getPaths("collection-object",null);
		assertArrayContainsString(names,p1);
		assertArrayContainsString(names,p2);
		assertArrayContainsString(names,p3);
	}
	
	@Test public void testSearch() throws Exception {
		deleteAll();
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj-search.json"));
		JSONObject restriction=new JSONObject();
		restriction.put("keywords","aardvark");
		String[] names=ss.getPaths("collection-object",restriction);
		assertArrayContainsString(names,p2);
		assertArrayDoesNotContainString(names,p1);		
	}
	
	@Test public void testMini() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("intake/",getJSON("int4.json"));
		JSONObject mini=ss.retrieveJSON("intake/"+p1+"/view");
		assertEquals("depositorX",mini.getString("summary"));
		assertEquals("entryNumberX",mini.getString("number"));		
	}
	
	@Test public void testAuthorityRefs() throws Exception {
		
	}
}
