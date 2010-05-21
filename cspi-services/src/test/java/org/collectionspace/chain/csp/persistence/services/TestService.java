package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;

public class TestService extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestService.class);
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	@Test public void testAssumptionMechanism() {
		log.info("Services Running!");
	}

	private String cspace305_hack(String in) {
		return in.replaceAll("/persons/","/collectionobjects/");
	}

	@Test public void testObjectsPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		log.info("got "+url.getURL());
		//assertTrue(url.getURL().startsWith("/collectionobjects/"));	// XXX should be, but CSPACE-305
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()),null,creds,cache);
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument("collectionobjects_common").selectSingleNode("collectionobjects_common/objectNumber").getText();
		assertEquals("2",num);
	}

	@Test public void testRolesPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/roles/",getDocument("obj5.xml"),creds,cache);
		assertEquals(201,url.getStatus());
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
	}

	@Test public void testPermissionsPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
		assertEquals(201,url.getStatus());
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
	}
	
	@Test public void testVocabPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/roles/",getDocument("obj5.xml"),creds,cache);
		assertEquals(201,url.getStatus());
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
	}

	@Test public void testObjectsPut() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.PUT,cspace305_hack(url.getURL()),buildObject("32","obj2.xml","collectionobjects_common"),creds,cache);
		assertEquals(201,url.getStatus()); // 201?
		doc=conn.getMultipartXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()),null,creds,cache);
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument("collectionobjects_common").selectSingleNode("collectionobjects_common/objectNumber").getText();
		assertEquals("32",num);
	}

	// TODO pre-emptive cache population
	
	private Map<String,Document> buildObject(String objid,String src,String part) throws DocumentException, IOException {
		InputStream data_stream=getResource(src);
		String data=IOUtils.toString(data_stream);
		data_stream.close();
		data=data.replaceAll("<<objnum>>",objid);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(new StringReader(data));
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put(part,doc);
		return parts;
	}
	
	@Test public void testDelete() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		ReturnedMultipartDocument doc1=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
		assertEquals(200,doc1.getStatus());		
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
		ReturnedMultipartDocument doc2=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
		assertEquals(404,doc2.getStatus());	 // XXX CSPACE-209, should be 404
		assertNull(doc2.getDocument("collectionobjects_common"));
	}
	
	@Test public void testSearch() throws Exception{
		// Insert one non-aardvark
		Map<String,Document> parts=new HashMap<String,Document>();
		Document doc1=getDocument("obj1.xml");
		parts.put("collectionobjects_common",doc1);		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		String non=url.getURLTail();
		// Insert one aardvark
		parts=new HashMap<String,Document>();
		Document doc2=getDocument("obj-search.xml");
		parts.put("collectionobjects_common",doc2);		
		url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		String good=url.getURLTail();		
		// search for aardvark
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.GET,"collectionobjects?kw=aardvark",null,creds,cache);
		assertEquals(200,doc.getStatus());
		Set<String> csids=new HashSet<String>();
		for(Node n : (List<Node>)doc.getDocument().selectNodes("collectionobjects-common-list/collection-object-list-item/csid")) {
			csids.add(n.getText());
		}
		assertFalse(csids.size()==0);
		assertTrue(csids.contains(good));
		assertFalse(csids.contains(non));
	}
}
