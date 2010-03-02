package org.collectionspace.chain.csp.persistence.services.relation;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestRelationsThroughWebapp {
	// XXX refactor
	protected InputStream getResource(String name) {
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
	
	private JSONObject makeRequest(JSONObject fields,JSONObject[] relations) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("fields",fields);
		if(relations!=null) {
			JSONArray r=new JSONArray();
			for(JSONObject s : relations)
				r.put(s);
			out.put("relations",r);
		}
		return out;
	}
	
	private JSONObject createMini(String type,String id) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("csid",id);
		out.put("recordtype",type);
		return out;
	}
	
	private JSONObject createRelation(String src_type,String src,String type,String dst_type,String dst,boolean one_way) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("source",createMini(src_type,src));
		out.put("target",createMini(dst_type,dst));
		out.put("type",type);
		out.put("one-way",one_way);
		return out;
	}
	
	private String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in),null).toString();
	}
	
	@Test public void testRelationsThroughWebapp() throws Exception {
		ServletTester jetty=setupJetty();
		// First create a couple of objects
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		String[] path1=id1.split("/");
		String[] path2=id2.split("/");		
		String[] path3=id3.split("/");
		// Now create a pair of relations in: 3<->1, 3->2: a. 1->3, b. 3->1, c. 3->2
		out=jettyDo(jetty,"POST","/chain/relationships",createRelation(path3[1],path3[2],"affects",path1[1],path1[2],false).toString());
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"POST","/chain/relationships",createRelation(path3[1],path3[2],"affects",path2[1],path2[2],true).toString());
		assertEquals(201,out.getStatus());	
		System.err.println("id1="+id1);
		System.err.println("id2="+id2);
		System.err.println("id3="+id3);
		// Check 1 has relation to 3
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		JSONObject data1=new JSONObject(out.getContent());
		//     that the destination is 3
		JSONArray rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(1,rel1.length());
		JSONObject mini1=rel1.getJSONObject(0);
		assertEquals("objects",mini1.getString("recordtype"));
		assertEquals(mini1.getString("csid"),path3[2]);
		String rida=mini1.getString("relid");
		//     pull the relation itself, and check it
		out=jettyDo(jetty,"GET","/chain/relationships/"+rida,null);
		JSONObject rd1=new JSONObject(out.getContent());
		assertEquals("affects",rd1.getString("type"));
		assertEquals(rida,rd1.getString("csid"));
		JSONObject src1=rd1.getJSONObject("source");
		assertEquals("objects",src1.getString("recordtype"));
		assertEquals(path1[2],src1.get("csid"));
		JSONObject dst1=rd1.getJSONObject("target");
		assertEquals("objects",dst1.getString("recordtype"));
		assertEquals(path3[2],dst1.get("csid"));
		
		
		
		
		System.err.println(out.getContent());
		
		
		
		/*
		 * OLD UDATE METHOD 
		 
		out=jettyDo(jetty,"POST","/chain/objects/",makeRequest(new JSONObject(getResourceString("obj3.json")),
															   new JSONObject[]{r1,r2}).toString());
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		System.err.println("id3="+id3);
		// Now do a retrieve on all three, check they have the right number of relations
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		assertEquals(200,out.getStatus());
		System.err.println(out.getContent());
		*/
	}
}
