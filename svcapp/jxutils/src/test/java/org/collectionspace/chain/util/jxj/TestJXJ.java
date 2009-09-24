package org.collectionspace.chain.util.jxj;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.util.jpath.InvalidJPathException;
import org.collectionspace.chain.util.jpath.JPathPath;
import org.collectionspace.chain.util.json.JSONUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
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
		assertEquals("COMMENTS",d1.getDocument().selectSingleNode("collection-object/comments").getText());
		assertEquals("DISTFEATURES",d1.getDocument().selectSingleNode("collection-object/distFeatures").getText());
		assertEquals("OBJNAME",d1.getDocument().selectSingleNode("collection-object/objectName").getText());
		assertEquals("DEPT",d1.getDocument().selectSingleNode("collection-object/responsibleDept").getText());
		assertTrue(JSONUtils.checkJSONEquiv(
				new JSONObject(d1.getDocument().selectSingleNode("collection-object/misc").getText()),
				new JSONObject("{\"misc1\":\"MISC1\",\"misc2\":\"MISC2\"}")));							
	}
	
	private void checkJSONValue(Object in,String jpath,Object value) throws InvalidJPathException, JSONException {
		JPathPath path=JPathPath.compile(jpath);
		Object ours=path.get(in);
		assertEquals(value,ours);
	}
	
	@Test public void testXMLToJSON() throws Exception {
		JXJFile translate=JXJFile.compile(getDocument("translations.xml"));
		Document input=getDocument("src2.xml");
		JXJTransformer t1=translate.getTransformer("collection-object");
		assertNotNull(t1);
		JSONObject d1=t1.xml2json(input);
		System.err.println(d1);
		checkJSONValue(d1,".title","TITLE");
		checkJSONValue(d1,".objectnumber","2");	
		JPathPath other_path=JPathPath.compile(".othernumber");
		Object others=other_path.get(d1);
		assertTrue(others instanceof JSONArray);
		JSONArray d2=(JSONArray)others;
		assertEquals(3,d2.length());
		assertEquals("3",d2.get(0));
		assertEquals("4",d2.get(1));
		assertEquals("5",d2.get(2));
		checkJSONValue(d1,".comments","COMMENTS");
		checkJSONValue(d1,".distfeatures","DISTFEATURES");
		checkJSONValue(d1,".objectname","OBJNAME");
		checkJSONValue(d1,".responsibledept","DEPT");
		checkJSONValue(d1,".misc2","MISC2");
		checkJSONValue(d1,".misc3","MISC3");
	}

	@Test public void testXMLMissingOkay() throws Exception {
		JXJFile translate=JXJFile.compile(getDocument("translations.xml"));
		Document input=getDocument("src3.xml");
		JXJTransformer t1=translate.getTransformer("collection-object");
		assertNotNull(t1);
		JSONObject d1=t1.xml2json(input);
		checkJSONValue(d1,".title","");
		checkJSONValue(d1,".objectnumber","");	
		JPathPath other_path=JPathPath.compile(".othernumber");
		Object others=other_path.get(d1);
		assertTrue(others instanceof JSONArray);
		JSONArray d2=(JSONArray)others;
		assertEquals(0,d2.length());
		checkJSONValue(d1,".comments","");
		checkJSONValue(d1,".distfeatures","");
		checkJSONValue(d1,".objectname","");
		checkJSONValue(d1,".responsibledept","");		
		System.err.println(d1.toString());
	}

	@Test public void testJSONMissingOkay() throws Exception {
		JXJFile translate=JXJFile.compile(getDocument("translations.xml"));
		JSONObject input=getJSON("src4.json");
		JXJTransformer t1=translate.getTransformer("collection-object");
		assertNotNull(t1);
		Document d1=t1.json2xml(input);
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/title").getText());
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/objectNumber").getText());
		assertEquals(0,d1.getDocument().selectNodes("collection-object/otherNumber").size());
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/comments").getText());
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/distFeatures").getText());
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/objectName").getText());
		assertEquals("",d1.getDocument().selectSingleNode("collection-object/responsibleDept").getText());
		System.err.println(d1.asXML());		
	}
}
