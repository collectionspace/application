package org.collectionspace.chain.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.harness.HarnessServlet;
import org.collectionspace.chain.schema.SchemaStore;
import org.collectionspace.chain.schema.StubSchemaStore;
import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.UnderlyingStorageException;
import org.collectionspace.chain.storage.UnimplementedException;
import org.collectionspace.chain.storage.file.FileStorage;
import org.collectionspace.chain.storage.file.StubJSONStore;
import org.collectionspace.chain.storage.services.ReturnedDocument;
import org.collectionspace.chain.storage.services.ServicesConnection;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestGeneral {
	
	private final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
	                        "\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
	                        "{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
	                        "\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
	                        "\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
	                        "\"param\":\"type\"}]}";
	
	private final static String testStr2 = "{\"a\":\"b\"}";
	
	
	private FileStorage store;
	private static String tmp=null;
	
	private static synchronized String tmpdir() {
		if(tmp==null) {
			tmp=System.getProperty("java.io.tmpdir");
		}
		return tmp;
	}
	
	private void rm_r(File in) {
		for(File f : in.listFiles()) {
			if(f.isDirectory())
				rm_r(f);
			f.delete();
		}
	}
	
	@Before public void setup() throws IOException {
		File tmp=new File(tmpdir());
		File dir=new File(tmp,"ju-cspace");
		if(dir.exists())
			rm_r(dir);
		if(!dir.exists())
			dir.mkdir();
		store=new FileStorage(dir.toString());
	}

	
	@Test public void writeJSONToFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/objects/json1.test", jsonObject);
	}
	
	@Test public void readJSONFromFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/objects/json1.test", jsonObject);
		JSONObject resultObj = store.retrieveJSON("/objects/json1.test");
		JSONObject testObj = new JSONObject(testStr);
		JSONTestUtil.assertJSONEquiv(resultObj,testObj);
	}

	@Test public void testJSONNotExist() throws JSONException, UnderlyingStorageException, UnimplementedException {
		try
		{
			store.retrieveJSON("nonesuch.json");
			assertTrue(false);
		}
		catch (ExistException onfe) {}
	}
	
	@Test public void testJSONUpdate() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr2);
		store.createJSON("/objects/json1.test", jsonObject);
		jsonObject = new JSONObject(testStr);
		store.updateJSON("/objects/json1.test", jsonObject);		
		JSONObject resultObj = store.retrieveJSON("/objects/json1.test");
		JSONObject testObj = new JSONObject(testStr);
		JSONTestUtil.assertJSONEquiv(resultObj,testObj);
	}

	@Test public void testJSONNoUpdateNonExisting() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		try {
			store.updateJSON("/objects/json1.test", jsonObject);
			assertTrue(false);
		} catch(ExistException e) {}
	}

	@Test public void testJSONNoCreateExisting() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/objects/json1.test", jsonObject);
		try {
			store.createJSON("/objects/json1.test", jsonObject);
		} catch(ExistException e) {}			
	}
	
	private File tmpSchemaFile(boolean sj) {
		File schema=new File(store.getStoreRoot()+"/schema");
		if(!schema.exists())
			schema.mkdir();
		return new File(schema,sj?"schema.json":"test-json-handle.tmp");
	}
	
	private void createSchemaFile(boolean sj) throws IOException {
		File file=tmpSchemaFile(sj);
		FileOutputStream out=new FileOutputStream(file);
		IOUtils.write(testStr2,out);
		out.close();
	}
	
	private void deleteSchemaFile(boolean sj) {
		File file=tmpSchemaFile(sj);
		file.delete();
	}
	
	@Test public void testSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot()+"/schema");
		createSchemaFile(false);
		JSONObject j=schema.getSchema("test-json-handle.tmp");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile(false);
	}
	
	@Test public void testDefaultingSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot()+"/schema");
		createSchemaFile(true);
		JSONObject j=schema.getSchema("");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile(true);
	}
	
	@Test public void testTrailingSlashOkayOnSchema() throws Exception {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot()+"/schema");
		createSchemaFile(true);
		JSONObject j=schema.getSchema("");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile(true);	
	}
	
	private ServletTester setupJetty() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("test-store",store.getStoreRoot());
		tester.start();
		return tester;
	}
	
	private String setupTestServer() throws Exception {
		ServletTester tester=new ServletTester();
		tester.setContextPath("/test");
		tester.addServlet(HarnessServlet.class,"/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("test-store",store.getStoreRoot());
		String connector=tester.createSocketConnector(true);
		System.err.println("connector="+connector);
		tester.start();
		return connector;
	}
	
	private HttpTester jettyDo(ServletTester tester,String method,String path,String data) throws IOException, Exception {
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod(method);
		request.setHeader("Host","tester");
		request.setURI(path);
		request.setVersion("HTTP/1.0");		
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	@Test public void testJettyStartupWorks() throws Exception {
		setupJetty();
	}
	
	@Test public void testSchemaGet() throws Exception {
		createSchemaFile(false);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema/test-json-handle.tmp",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile(false);
		assertEquals(testStr2,out.getContent());
	}
	
	@Test public void testDefaultingSchemaGet() throws Exception {	
		createSchemaFile(true);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile(true);
		assertEquals(testStr2,out.getContent());
	}

	@Test public void testDefaultingSchemaGetWithTrailingSlash() throws Exception {	
		createSchemaFile(true);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema/",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile(true);
		assertEquals(testStr2,out.getContent());
	}
	
	@Test public void testSchemaPostAndDelete() throws Exception {
		deleteSchemaFile(false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/test-json-handle.tmp",testStr2);	
		assertEquals(out.getMethod(),null);
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertEquals(testStr2,out.getContent());
		out=jettyDo(jetty,"PUT","/chain/objects/test-json-handle.tmp",testStr);
		assertEquals(200,out.getStatus());		
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertEquals(testStr,out.getContent());		
		out=jettyDo(jetty,"DELETE","/chain/objects/test-json-handle.tmp",null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
	}
	
	@Test public void testServeStatic() throws Exception {
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/chain.properties",null);
		assertEquals(200,out.getStatus());
		assertTrue(out.getContent().contains("cspace.chain.store.dir"));
	}
	
	@Test public void testObjectList() throws Exception {
		ServletTester jetty=setupJetty();
		jettyDo(jetty,"POST","/chain/objects/a",testStr2);	
		jettyDo(jetty,"POST","/chain/objects/b",testStr2);	
		jettyDo(jetty,"POST","/chain/objects/c",testStr2);
		File storedir=new File(store.getStoreRoot(),"store");
		if(!storedir.exists())
			storedir.mkdir();
		File junk=new File(storedir,"junk");
		IOUtils.write("junk",new FileOutputStream(junk));	
		HttpTester out=jettyDo(jetty,"GET","/chain/objects",null);
		assertEquals(200,out.getStatus());
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();
		for(int i=0;i<items.length();i++)
			files.add(items.getString(i));
		assertTrue(files.contains("a"));
		assertTrue(files.contains("b"));
		assertTrue(files.contains("c"));
		assertEquals(3,files.size());
	}
	
	@Test public void testXMLDocumentRetrieve() throws Exception {
		String base=setupTestServer();
		ServicesConnection conn=new ServicesConnection(base+"/test");
		ReturnedDocument retdoc=conn.getXMLDocument(RequestMethod.GET,"test.xml");
		assertEquals(200,retdoc.getStatus());
		Document doc=retdoc.getDocument();
		assertEquals(1,doc.selectNodes("test").size());
	}
	
	private Document makeXML(String in) throws DocumentException, UnsupportedEncodingException {
		SAXReader reader=new SAXReader();
		return reader.read(new ByteArrayInputStream(in.getBytes("UTF-8")));
	}
	
	@Test public void testReflectTest() throws Exception {
		String base=setupTestServer();
		ServicesConnection conn=new ServicesConnection(base+"/test/");
		ReturnedDocument retdoc=conn.getXMLDocument(RequestMethod.POST,"/reflect",makeXML("<hello/>"));
		assertEquals(200,retdoc.getStatus());
		Document doc=retdoc.getDocument();
		assertEquals(1,doc.selectNodes("hello").size());		
	}
	
	@Test public void testPutReturnsContent() throws Exception {
		deleteSchemaFile(false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/test-json-handle.tmp",testStr2);	
		assertEquals(out.getMethod(),null);
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/test-json-handle.tmp",null);
		assertEquals(testStr2,out.getContent());
		out=jettyDo(jetty,"PUT","/chain/objects/test-json-handle.tmp",testStr);
		assertEquals(200,out.getStatus());	
		assertEquals(testStr,out.getContent());	
	}
	
	@Test public void testTrailingSlashOkayOnList() throws Exception {
		ServletTester jetty=setupJetty();
		jettyDo(jetty,"POST","/chain/objects/a",testStr2);	
		jettyDo(jetty,"POST","/chain/objects/b",testStr2);	
		jettyDo(jetty,"POST","/chain/objects/c",testStr2);
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/",null);
		assertEquals(200,out.getStatus());
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();
		for(int i=0;i<items.length();i++)
			files.add(items.getString(i));
		assertTrue(files.contains("a"));
		assertTrue(files.contains("b"));
		assertTrue(files.contains("c"));
		assertEquals(3,files.size());		
	}
	
	@Test public void testDirectories() throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/a/json1.test", jsonObject);
		store.createJSON("/b/json2.test", jsonObject);
		File d1=new File(store.getStoreRoot());
		assertTrue(d1.exists());
		File d2=new File(d1,"data");
		assertTrue(d2.exists());
		File a=new File(d2,"a");
		assertTrue(a.exists());
		File b=new File(d2,"b");
		assertTrue(b.exists());
		assertTrue(new File(a,"json1.test.json").exists());
		assertTrue(new File(b,"json2.test.json").exists());
	}
}
