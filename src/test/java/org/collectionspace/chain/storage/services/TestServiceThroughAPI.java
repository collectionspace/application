package org.collectionspace.chain.storage.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.storage.services.ReturnedDocument;
import org.collectionspace.chain.storage.services.ReturnedURL;
import org.collectionspace.chain.storage.services.ServicesConnection;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestServiceThroughAPI {
	private static String BASE_URL="http://chalk-233:8080"; // XXX configure
	private ServicesConnection conn;
	private Random rnd=new Random();
	
	// XXX refactor
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}
	
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
	
	@Before public void checkServicesRunning() throws BadRequestException {
		try {
			conn=new ServicesConnection(BASE_URL+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
		} catch(BadRequestException e) {
			Assume.assumeTrue(false);
		}
	}
	
	@Test public void testObjectsPost() throws Exception {
		ServicesStorage ss=new ServicesStorage(BASE_URL+"/helloworld/cspace-nuxeo/");
		String name=ss.autocreateJSON("collection-object",getJSON("obj3.json"));
		System.err.println("name="+name);
	}
}
