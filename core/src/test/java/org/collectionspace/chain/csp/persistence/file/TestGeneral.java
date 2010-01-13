package org.collectionspace.chain.csp.persistence.file;

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
import org.collectionspace.chain.csp.persistence.file.FileStorage;
import org.collectionspace.chain.csp.persistence.file.StubJSONStore;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.chain.uispec.StubSchemaStore;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
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

	private final static String testStr2 = "{\"a\":\"b\\\"\"}";

	private final static String testStr3 = "{\"a\":\"b\",\"id\":\"***misc***\",\"objects\":\"***objects***\",\"intake\":\"***intake***\"}";
	
	private final static String testStr4 = "{\"a\":\"b\",\"id\":\"MISC2009.1\",\"objects\":\"OBJ2009.1\",\"intake\":\"IN2009.1\"}";
	private final static String testStr5 = "{\"a\":\"b\",\"id\":\"MISC2009.2\",\"objects\":\"OBJ2009.2\",\"intake\":\"IN2009.2\"}";
	
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

	@Before public void setup() throws IOException, CSPDependencyException {
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
		store.autocreateJSON("/objects/", jsonObject);
	}

	@Test public void readJSONFromFile() throws JSONException, ExistException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		String path=store.autocreateJSON("/objects/", jsonObject);
		JSONObject resultObj = store.retrieveJSON("/objects/"+path);
		JSONObject testObj = new JSONObject(testStr);
		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
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
		String id1=store.autocreateJSON("/objects/", jsonObject);
		jsonObject = new JSONObject(testStr);
		store.updateJSON("/objects/"+id1, jsonObject);		
		JSONObject resultObj = store.retrieveJSON("/objects/"+id1);
		JSONObject testObj = new JSONObject(testStr);
		assertTrue(JSONUtils.checkJSONEquiv(resultObj,testObj));
	}

	@Test public void testJSONNoUpdateNonExisting() throws ExistException, JSONException, UnderlyingStorageException, UnimplementedException {
		JSONObject jsonObject = new JSONObject(testStr);
		try {
			store.updateJSON("/objects/json1.test", jsonObject);
			assertTrue(false);
		} catch(ExistException e) {}
	}

	private File tmpSchemaFile(String type,boolean sj) {
		File sroot=new File(store.getStoreRoot()+"/schemas");
		if(!sroot.exists())
			sroot.mkdir();
		File schema=new File(store.getStoreRoot()+"/schemas/"+type);
		if(!schema.exists())
			schema.mkdir();
		return new File(schema,sj?"schema.json":"test-json-handle.tmp");
	}

	private void createSchemaFile(String type,boolean sj,boolean alt) throws IOException {
		File file=tmpSchemaFile(type,sj);
		FileOutputStream out=new FileOutputStream(file);
		IOUtils.write(alt?testStr2:testStr,out);
		out.close();
	}

	private void deleteSchemaFile(String type,boolean sj) {
		File file=tmpSchemaFile(type,sj);
		file.delete();
	}

	@Test public void testSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",false,true);
		JSONObject j=schema.getSchema("collection-object/test-json-handle.tmp");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile("collection-object",false);
	}

	@Test public void testDefaultingSchemaStore() throws IOException, JSONException {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",true,true);
		JSONObject j=schema.getSchema("collection-object");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile("collection-object",true);
	}

	@Test public void testTrailingSlashOkayOnSchema() throws Exception {
		SchemaStore schema=new StubSchemaStore(store.getStoreRoot());
		createSchemaFile("collection-object",true,true);
		JSONObject j=schema.getSchema("collection-object/");
		assertEquals(testStr2,j.toString());
		deleteSchemaFile("collection-object",true);	
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
		createSchemaFile("collection-object",false,true);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema/test-json-handle.tmp",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile("collection-object",false);
		assertEquals(testStr2,out.getContent());
	}

	@Test public void testDefaultingSchemaGet() throws Exception {	
		createSchemaFile("collection-object",true,true);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile("collection-object",true);
		assertEquals(testStr2,out.getContent());
	}

	@Test public void testDefaultingSchemaGetWithTrailingSlash() throws Exception {	
		createSchemaFile("collection-object",true,true);
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/objects/schema/",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		deleteSchemaFile("collection-object",true);
		assertEquals(testStr2,out.getContent());
	}

	private JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		return out;
	}
	
	private String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}
	
	@Test public void testAutoPost() throws Exception {
		deleteSchemaFile("collection-object",false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
		assertNotNull(out.getHeader("Location"));
		assertTrue(out.getHeader("Location").startsWith("/objects/"));
		Integer.parseInt(out.getHeader("Location").substring("/objects/".length()));
	}
	
	private String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	private JSONObject getFields(JSONObject in) throws JSONException {
		in=in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}
	
	@Test public void testPostAndDelete() throws Exception {
		deleteSchemaFile("collection-object",false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
		assertEquals(out.getMethod(),null);
		String id=out.getHeader("Location");
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(testStr));
		assertEquals(200,out.getStatus());		
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
	}

	@Test public void testMultipleStoreTypes() throws Exception {
		deleteSchemaFile("collection-object",false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
		assertEquals(out.getMethod(),null);
		String id1=out.getHeader("Location");
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(testStr));	
		assertEquals(out.getMethod(),null);
		String id2=out.getHeader("Location");		
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());		
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
		out=jettyDo(jetty,"DELETE","/chain"+id1,null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		assertTrue(out.getStatus()>=400); // XXX should probably be 404
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr)));
	}

	@Test public void testAutoGet() throws Exception {
		deleteSchemaFile("collection-object",false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/__auto",makeSimpleRequest(testStr3));
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/__auto",null);
		// XXX algorithm will change.
		System.err.println(out.getContent());
		JSONObject data=new JSONObject(out.getContent());
		JSONUtils.checkJSONEquiv(data,testStr4);
		out=jettyDo(jetty,"GET","/chain/objects/__auto",null);
		data=new JSONObject(out.getContent());
		JSONUtils.checkJSONEquiv(data,testStr5);		
	}
	
	@Test public void testMultipleSchemas() throws Exception {
		ServletTester jetty=setupJetty();
		createSchemaFile("collection-object",false,true);
		createSchemaFile("intake",false,false);
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/schema/test-json-handle.tmp",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		assertTrue(JSONUtils.checkJSONEquiv(new JSONObject(testStr2),new JSONObject(out.getContent())));
		out=jettyDo(jetty,"GET","/chain/intake/schema/test-json-handle.tmp",null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		assertTrue(JSONUtils.checkJSONEquiv(new JSONObject(testStr),new JSONObject(out.getContent())));
	}

	@Test public void testServeStatic() throws Exception {
		HttpTester out=jettyDo(setupJetty(),"GET","/chain/chain.properties",null);
		assertEquals(200,out.getStatus());
		assertTrue(out.getContent().contains("cspace.chain.store.dir"));
	}
	
	@Test public void testObjectList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out1=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
		HttpTester out2=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
		HttpTester out3=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
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
			files.add("/objects/"+items.getJSONObject(i).getString("csid"));
		System.err.println(out1.getHeader("Location"));
		assertTrue(files.contains(out1.getHeader("Location")));
		assertTrue(files.contains(out2.getHeader("Location")));
		assertTrue(files.contains(out3.getHeader("Location")));
		assertEquals(3,files.size());
	}

	@Test public void testPutReturnsContent() throws Exception {
		deleteSchemaFile("collection-object",false);
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
		String id=out.getHeader("Location");
		assertEquals(out.getMethod(),null);
		System.err.println(out.getContent());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getFields(out.getContent())),new JSONObject(testStr2)));
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(testStr));
		assertEquals(200,out.getStatus());	
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(testStr),new JSONObject(getFields(out.getContent()))));
	}

	@Test public void testTrailingSlashOkayOnList() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out1=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
		HttpTester out2=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));	
		HttpTester out3=jettyDo(jetty,"POST","/chain/objects",makeSimpleRequest(testStr2));
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/",null);
		assertEquals(200,out.getStatus());
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();
		for(int i=0;i<items.length();i++)
			files.add("/objects/"+items.getJSONObject(i).getString("csid"));
		assertTrue(files.contains(out1.getHeader("Location")));
		assertTrue(files.contains(out2.getHeader("Location")));
		assertTrue(files.contains(out3.getHeader("Location")));
		assertEquals(3,files.size());		
	}

	@Test public void testDirectories() throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject jsonObject = new JSONObject(testStr);
		String id1=store.autocreateJSON("/a", jsonObject);
		String id2=store.autocreateJSON("/b", jsonObject);
		File d1=new File(store.getStoreRoot());
		assertTrue(d1.exists());
		File d2=new File(d1,"data");
		assertTrue(d2.exists());
		File a=new File(d2,"a");
		assertTrue(a.exists());
		File b=new File(d2,"b");
		assertTrue(b.exists());
		assertTrue(new File(a,id1+".json").exists());
		assertTrue(new File(b,id2+".json").exists());
	}
	
	@Test public void testReset() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));	
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(testStr2));
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/reset",null);
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain/objects/",null);
		assertEquals(200,out.getStatus());
		JSONObject result=new JSONObject(out.getContent());
		JSONArray items=result.getJSONArray("items");
		Set<String> files=new HashSet<String>();
		String csid0=null;
		String accnum0=null;
		for(int i=0;i<items.length();i++) {
			files.add(items.getJSONObject(i).getString("accessionNumber"));
			if(i==0) {
				csid0=items.getJSONObject(i).getString("csid");
				accnum0=items.getJSONObject(i).getString("accessionNumber");
			}
		}
		assertTrue(files.contains("1984.068.0335b"));
		assertTrue(files.contains("1984.068.0338"));
		out=jettyDo(setupJetty(),"GET","/chain/objects/"+csid0,null);
		assertEquals(out.getMethod(),null);
		assertEquals(200,out.getStatus());
		String cmp=IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/collectionspace/chain/controller/reset-objects/"+accnum0+".json"));
		JSONObject cmp1=new JSONObject(cmp);
		JSONObject cmp2=new JSONObject(out.getContent());
		cmp1.remove("csid");
		cmp2=getFields(cmp2);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(cmp1,cmp2));
		assertEquals(4,files.size());		
	}
	
	@Test public void testLogin() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/login?userid=guest&password=guest",null);	
		assertEquals(303,out.getStatus());
		assertEquals("/cspace-ui/html/createnew.html",out.getHeader("Location"));
		out=jettyDo(jetty,"GET","/chain/login?userid=curator&password=curator",null);
		assertEquals(303,out.getStatus());
		assertFalse(out.getHeader("Location").endsWith("?result=fail"));
		out=jettyDo(jetty,"GET","/chain/login?userid=admin&password=admin",null);
		assertEquals(303,out.getStatus());
		assertFalse(out.getHeader("Location").endsWith("?result=fail"));
		out=jettyDo(jetty,"GET","/chain/login?userid=guest&password=toast",null);	
		assertEquals(303,out.getStatus());
		assertTrue(out.getHeader("Location").endsWith("?result=fail"));
		out=jettyDo(jetty,"GET","/chain/login?userid=bob&password=bob",null);	
		assertEquals(303,out.getStatus());
		assertTrue(out.getHeader("Location").endsWith("?result=fail"));
	}
}
