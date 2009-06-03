package org.collectionspace.chain.test;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.collectionspace.chain.services.ReturnedDocument;
import org.collectionspace.chain.services.ReturnedURL;
import org.collectionspace.chain.services.ServicesConnection;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestService {
	private static String BASE_URL="http://chalk-233:8080";
	private ServicesConnection conn;
	
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}
	
	@Before public void checkServicesRunning() throws BadRequestException {
		try {
			conn=new ServicesConnection(BASE_URL+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
		} catch(BadRequestException e) {
			Assume.assumeTrue(false);
		}
	}
	
	@Test public void testAssumptionMechanism() {
		System.err.println("Services Running!");
	}

	@Test public void testObjectsPost() throws Exception {
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		System.err.println("got "+url.getURL());
		assertTrue(url.getURL().startsWith("/collectionobjects/"));		
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc.getStatus());
	}
}
