package org.collectionspace.chain.storage;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.csp.persistence.services.ServicesStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestServiceThroughWebapp {
	// XXX refactor
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	// XXX refactor
	private String getResourceString(String name) throws IOException {
		InputStream in=getResource(name);
		return IOUtils.toString(in);
	}
	
	// XXX refactor
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
	
	// XXX refactor into other copy of this method
	private ServletTester setupJetty() throws Exception {
		BootstrapConfigController config_controller=new BootstrapConfigController(null);
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.go();
		String base=config_controller.getOption("services-url");		
		ServletTester tester=new ServletTester();
		tester.setContextPath("/chain");
		tester.addServlet(ChainServlet.class, "/*");
		tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
		tester.setAttribute("storage","service");
		tester.setAttribute("store-url",base+"/cspace-services/");		
		tester.start();
		return tester;
	}
	
	private String getFields(String in) throws JSONException {
		return getFields(new JSONObject(in)).toString();
	}

	private JSONObject getFields(JSONObject in) throws JSONException {
		in=in.getJSONObject("fields");
		in.remove("csid");
		return in;
	}
	
	private JSONObject makeRequest(JSONObject fields) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		return out;
	}
	
	private String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in)).toString();
	}
	
	@Test public void testCollectionObjectBasic() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));	
		String id=out.getHeader("Location");
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("obj3.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(getResourceString("obj4.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("obj4.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404
	}

	@Test public void testIntake() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("int3.json")));	
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=jettyDo(jetty,"GET","/chain"+path,null);
		System.err.println(out.getContent());
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int3.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+path,makeSimpleRequest(getResourceString("int4.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+path,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int4.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+path,null);
		out=jettyDo(jetty,"GET","/chain"+path,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404		
	}

	@Test public void testAcquisition() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/acquisition/",makeSimpleRequest(getResourceString("int5.json")));	
		assertEquals(out.getMethod(),null);
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=jettyDo(jetty,"GET","/chain"+path,null);
		System.err.println(out.getContent());
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int5.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+path,makeSimpleRequest(getResourceString("int6.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+path,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int6.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+path,null);
		out=jettyDo(jetty,"GET","/chain"+path,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404		
	}
	
	@Test public void testIDGenerate() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/id/intake",null);
		JSONObject jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("IN2009."));
		out=jettyDo(jetty,"GET","/chain/id/objects",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("2009.1."));
	}

	@Test public void testAutoGet() throws Exception {
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"GET","/chain/objects/__auto",null);
		assertEquals(200,out.getStatus());
		// XXX this is correct currently, whilst __auto is stubbed.
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(),new JSONObject(out.getContent())));
	}
	
	@Test public void testList() throws Exception {
		ServletTester jetty=setupJetty();
		// delete all
		HttpTester out=jettyDo(jetty,"GET","/chain/objects",null);
		assertEquals(200,out.getStatus());
		JSONObject in=new JSONObject(out.getContent());
		JSONArray items=in.getJSONArray("items");
		for(int i=0;i<items.length();i++) {
			JSONObject data=items.getJSONObject(i);
			out=jettyDo(jetty,"DELETE","/chain/objects/"+data.getString("csid"),null);
			assertEquals(200,out.getStatus());
		}
		// empty
		out=jettyDo(jetty,"GET","/chain/objects",null);
		assertEquals(200,out.getStatus());
		in=new JSONObject(out.getContent());
		items=in.getJSONArray("items");
		assertEquals(0,items.length());
		// put a couple in
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));	
		String id1=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));	
		String id2=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		// size 2, right ones, put them in the right place
		out=jettyDo(jetty,"GET","/chain/objects",null);
		assertEquals(200,out.getStatus());
		in=new JSONObject(out.getContent());
		items=in.getJSONArray("items");
		assertEquals(2,items.length());
		JSONObject obj1=items.getJSONObject(0);
		JSONObject obj2=items.getJSONObject(1);
		if(id2.split("/")[2].equals(obj1.getString("csid"))) {
			JSONObject t=obj1;
			obj1=obj2;
			obj2=t;
		}
		// check
		assertEquals(id1.split("/")[2],obj1.getString("csid"));
		assertEquals(id2.split("/")[2],obj2.getString("csid"));
		assertEquals("objects",obj1.getString("recordtype"));
		assertEquals("objects",obj2.getString("recordtype"));
		assertEquals("TITLE",obj1.getString("summary"));
		assertEquals("TITLE",obj2.getString("summary"));
		assertEquals("OBJNUM",obj1.getString("number"));
		assertEquals("OBJNUM",obj2.getString("number"));
	}
}
