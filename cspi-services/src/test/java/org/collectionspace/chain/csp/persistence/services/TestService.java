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

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;

public class TestService extends ServicesBaseClass {
	private Random rnd=new Random();

	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}

	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	@Test public void testAssumptionMechanism() {
		System.err.println("Services Running!");
	}

	private String cspace305_hack(String in) {
		return in.replaceAll("/persons/","/collectionobjects/");
	}

	@Test public void testObjectsPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts);
		assertEquals(201,url.getStatus());
		System.err.println("got "+url.getURL());
		//assertTrue(url.getURL().startsWith("/collectionobjects/"));	// XXX should be, but CSPACE-305
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()),null);
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument("collectionobjects_common").selectSingleNode("collectionobjects_common/objectNumber").getText();
		assertEquals("2",num);
	}

	@Test public void testObjectsPut() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts);
		assertEquals(201,url.getStatus());
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.PUT,cspace305_hack(url.getURL()),buildObject("32","obj2.xml","collectionobjects_common"));
		assertEquals(201,url.getStatus()); // 201?
		doc=conn.getMultipartXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()),null);
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument("collectionobjects_common").selectSingleNode("collectionobjects_common/objectNumber").getText();
		assertEquals("32",num);
	}

	// TODO pre-emptive cache population

	// Speeds up many tests, ensures others work at all
	@SuppressWarnings("unchecked")
	private void deleteAll(String type,String item) throws Exception {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,type+"/");
		if(all.getStatus()!=200)
			throw new ConnectionException("Bad request during identifier cache map update: status not 200");
		List<Node> objects=all.getDocument().selectNodes(item);
		for(Node object : objects) {
			String csid=object.selectSingleNode("csid").getText();
			conn.getNone(RequestMethod.DELETE,type+"/"+csid,null);
		}
	}

	private void deleteAll() throws Exception {
		deleteAll("collectionobjects","collectionobjects-common-list/collection-object-list-item");
		deleteAll("intakes","intakes-common-list/intake-list-item");
	}
	
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

	@Test public void testSetvicesIdentifierMapBasic() throws Exception {
		deleteAll(); // for speed
		ServicesIdentifierMap sim=
			new ServicesIdentifierMap(conn,
					"collectionobjects",
					"collectionobjects-common-list/collection-object-list-item",
					"collectionobjects_common/objectNumber","collectionobjects_common");
		String objid="test-sim-"+rnd.nextInt(Integer.MAX_VALUE);
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",buildObject(objid,"obj2.xml","collectionobjects_common"));
		assertEquals(201,url.getStatus());
		String csid=url.getURL().substring(url.getURL().lastIndexOf("/")+1);
		String csid2=sim.getCSID(objid);
		assertEquals(csid,csid2);
		String csid3=sim.getCSID(objid);
		assertEquals(csid,csid3);
		assertEquals(1,sim.getNumberHits());
		assertEquals(1,sim.getNumberMisses());
		assertEquals(1,sim.getLoadSteps());
		String objid2=sim.fromCSID(csid);
		assertEquals(objid,objid2);
		assertEquals(2,sim.getNumberHits());
	}

	@Test public void testSetvicesIdentifierMapBasic2() throws Exception {
		deleteAll(); // for speed
		ServicesIdentifierMap sim=
			new ServicesIdentifierMap(conn,
					"intakes",
					"intakes-common-list/intake-list-item",
					"intakes_common/entryNumber","intakes_common");
		String objid="test-sim-"+rnd.nextInt(Integer.MAX_VALUE);
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"intakes/",buildObject(objid,"obj-intake.xml","intakes_common"));
		assertEquals(201,url.getStatus());
		String csid=url.getURL().substring(url.getURL().lastIndexOf("/")+1);
		String csid2=sim.getCSID(objid);
		assertEquals(csid,csid2);
		String csid3=sim.getCSID(objid);
		assertEquals(csid,csid3);
		assertEquals(1,sim.getNumberHits());
		assertEquals(1,sim.getNumberMisses());
		assertEquals(1,sim.getLoadSteps());
		String objid2=sim.fromCSID(csid);
		assertEquals(objid,objid2);
		assertEquals(2,sim.getNumberHits());
	}
	
	@Test public void testDelete() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts);
		assertEquals(201,url.getStatus());
		ReturnedMultipartDocument doc1=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null);
		assertEquals(200,doc1.getStatus());		
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
		ReturnedMultipartDocument doc2=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null);
		assertEquals(404,doc2.getStatus());	 // XXX CSPACE-209, should be 404
		assertNull(doc2.getDocument("collectionobjects_common"));
	}
	
	// XXX temporary venus test: migrate to proper test of multipart
	@Test public void testPostMultipartDocs() throws Exception {
		ServicesConnection sc=new ServicesConnection("http://venus.collectionspace.org:8180/cspace-services/");
		Map<String,Document> documents=new HashMap<String,Document>();
		documents.put("collectionobjects_common",getDocument("obj1.xml"));
		documents.put("collectionobjects_naturalhistory",getDocument("obj-nh.xml"));
		
		ReturnedURL docs=sc.getMultipartURL(RequestMethod.POST,"collectionobjects/",documents);
		assertEquals(201,docs.getStatus());
		System.err.println(docs.getURL());
	}
}
