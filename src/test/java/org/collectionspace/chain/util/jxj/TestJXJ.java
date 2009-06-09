package org.collectionspace.chain.util.jxj;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TestJXJ {
	private Document getDocument(String in) throws DocumentException, IOException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		assertNotNull(stream);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(stream);
		stream.close();
		return doc;
	}
	
	private JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		assertNotNull(stream);
		String data=IOUtils.toString(stream);
		stream.close();		
		return new JSONObject(data);
	}
	
	@Test public void testJSONToXML() throws Exception {
		JXJFile translate=JXJFile.compile(getDocument("translations.xml"));
		JSONObject input=getJSON("src1.json");
		JXJTransformer t1=translate.getTransformer("collection-object");
		assertNotNull(t1);
		Document d1=t1.json2xml(input);
		assertEquals("TITLE",d1.getDocument().selectSingleNode("collection-object/title").getText());
		assertEquals("OBJNUM",d1.getDocument().selectSingleNode("collection-object/objectNumber").getText());
		assertEquals(3,d1.getDocument().selectNodes("collection-object/otherNumber").size());
		assertEquals("2",((Node)(d1.getDocument().selectNodes("collection-object/otherNumber").get(1))).getText());
		System.err.println(d1.asXML());
		// XXX
	}
	
	@Test public void testXMLToJSON() throws Exception {
		JXJFile translate=JXJFile.compile(getDocument("translations.xml"));
		Document input=getDocument("src2.xml");
		JXJTransformer t1=translate.getTransformer("collection-object");
		assertNotNull(t1);
		JSONObject d1=t1.xml2json(input);
		System.err.println(d1.toString());
	}
}
