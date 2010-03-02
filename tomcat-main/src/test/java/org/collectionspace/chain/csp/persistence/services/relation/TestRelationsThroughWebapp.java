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
		// Check that 2 has no relations at all
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		JSONObject data2=new JSONObject(out.getContent());
		//     that the destination is 3
		JSONArray rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(0,rel2.length());
		// Check that 3 has relations to 1 and 2
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		JSONObject data3=new JSONObject(out.getContent());
		//     untangle them
		JSONArray rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(2,rel3.length());
		int i1=0,i0=1;
		String rel_a=rel3.getJSONObject(i1).getString("csid");
		String rel_b=rel3.getJSONObject(i0).getString("csid");
		if(rel_a.equals(id2) && rel_b.equals(id1)) {
			i0=0;
			i1=1;
		}
		JSONObject rel31=rel3.getJSONObject(i0);
		JSONObject rel32=rel3.getJSONObject(i1);		
		//     check desintations
		assertEquals("objects",rel31.getString("recordtype"));
		assertEquals(rel31.getString("csid"),path1[2]);
		String rid31=rel31.getString("relid");
		assertEquals("objects",rel32.getString("recordtype"));
		assertEquals(rel32.getString("csid"),path2[2]);
		String rid32=rel32.getString("relid");
		//    check actual records
		//        3 -> 1
		out=jettyDo(jetty,"GET","/chain/relationships/"+rid31,null);
		JSONObject rd31=new JSONObject(out.getContent());
		assertEquals("affects",rd31.getString("type"));
		assertEquals(rid31,rd31.getString("csid"));
		JSONObject src31=rd31.getJSONObject("source");
		assertEquals("objects",src31.getString("recordtype"));
		assertEquals(path3[2],src31.get("csid"));
		JSONObject dst31=rd31.getJSONObject("target");
		assertEquals("objects",dst31.getString("recordtype"));
		assertEquals(path1[2],dst31.get("csid"));
		//        3 -> 2
		out=jettyDo(jetty,"GET","/chain/relationships/"+rid32,null);
		JSONObject rd32=new JSONObject(out.getContent());
		assertEquals("affects",rd32.getString("type"));
		assertEquals(rid32,rd32.getString("csid"));
		JSONObject src32=rd32.getJSONObject("source");
		assertEquals("objects",src32.getString("recordtype"));
		assertEquals(path3[2],src32.get("csid"));
		JSONObject dst32=rd32.getJSONObject("target");
		assertEquals("objects",dst32.getString("recordtype"));
		assertEquals(path2[2],dst32.get("csid"));		
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
	
	// XXX factor out creation
	@Test public void testRelationsMissingOneWayThroughWebapp() throws Exception {
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
		JSONObject data=createRelation(path3[1],path3[2],"affects",path1[1],path1[2],false);
		data.remove("one-way");
		out=jettyDo(jetty,"POST","/chain/relationships",data.toString());
		assertEquals(201,out.getStatus());
		// Just heck they have length 1 (other stuff will be tested by main test)
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		JSONObject data3=new JSONObject(out.getContent());
		JSONArray rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(1,rel3.length());
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		JSONObject data1=new JSONObject(out.getContent());
		JSONArray rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(1,rel1.length());
	}
	
	@Test public void testMultipleCreateThroughWebapp() throws Exception {
		ServletTester jetty=setupJetty();
		// Create test objects
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
		// Do the rleation
		JSONObject data1=createRelation(path3[1],path3[2],"affects",path1[1],path1[2],false);
		JSONObject data2=createRelation(path3[1],path3[2],"affects",path2[1],path2[2],false);
		JSONArray datas=new JSONArray();
		datas.put(data1);
		datas.put(data2);
		JSONObject data=new JSONObject();
		data.put("items",datas);
		out=jettyDo(jetty,"POST","/chain/relationships",data.toString());
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		JSONObject data3=new JSONObject(out.getContent());
		JSONArray rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(2,rel3.length());

	}
}
