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
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
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
		
		
		// XXX retrieve by authority
		// XXX also authorities
	}
}
