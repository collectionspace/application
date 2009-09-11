package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.collectionspace.kludge.*;

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
	
	@Before public void checkServicesRunning() throws ConfigLoadFailedException, ConnectionException {
		setup();
	}
	
	@Test public void testAssumptionMechanism() {
		System.err.println("Services Running!");
	}

	private String cspace305_hack(String in) {
		return in.replaceAll("/persons/","/collectionobjects/");
	}
	
	@Test public void testObjectsPost() throws Exception {
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		System.err.println("got "+url.getURL());
		//assertTrue(url.getURL().startsWith("/collectionobjects/"));	// XXX should be, but CSPACE-305
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()));
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument().selectSingleNode("collection-object/objectNumber").getText();
		assertEquals("2",num);
	}

	@Test public void testObjectsPut() throws Exception {
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.PUT,cspace305_hack(url.getURL()),buildObject("32"));
		assertEquals(201,url.getStatus()); // 201?
		doc=conn.getXMLDocument(RequestMethod.GET,cspace305_hack(url.getURL()));
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument().selectSingleNode("collection-object/objectNumber").getText();
		assertEquals("32",num);
	}
	
	// TODO pre-emptive cache population
	
	// Speeds up many tests, ensures others work at all
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
	
	private Document buildObject(String objid) throws DocumentException, IOException {
		InputStream data_stream=getResource("obj2.xml");
		String data=IOUtils.toString(data_stream);
		data_stream.close();
		data=data.replaceAll("<<objnum>>",objid);
		SAXReader reader=new SAXReader();
		return reader.read(new StringReader(data));
	}
	
	@Test public void testSetvicesIdentifierMapBasic() throws Exception {
		deleteAll(); // for speed
		ServicesIdentifierMap sim=new ServicesIdentifierMap(conn);
		String objid="test-sim-"+rnd.nextInt(Integer.MAX_VALUE);
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",buildObject(objid));
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
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		ReturnedDocument doc1=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc1.getStatus());		
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null);
		assertEquals(204,status); // XXX CSPACE-73, should be 404
		ReturnedDocument doc2=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc2.getStatus());	 // XXX CSPACE-209, should be 404
		assertEquals(0,doc2.getDocument().selectNodes("collection-object/*").size());
	}
}
