package org.collectionspace.chain.csp.persistence.services.relation;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.storage.UTF8SafeHttpTester;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX refactor like mad

public class TestRelationsThroughWebapp {
	private static final Logger log=LoggerFactory.getLogger(TestRelationsThroughWebapp.class);
	private String cookie;
	
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
		if(cookie!=null)
			request.addHeader(HttpHeaders.COOKIE,cookie);
		if(data!=null)
			request.setContent(data);
		response.parse(tester.getResponses(request.generate()));
		return response;
	}
	
	private void login(ServletTester tester) throws IOException, Exception {
		HttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
		assertEquals(303,out.getStatus());
		cookie=out.getHeader("Set-Cookie");
		log.info("Got cookie "+cookie);
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
		tester.setAttribute("config-filename","default.xml");
		tester.start();
		login(tester);
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
		
	@Test public void testRelationsCreate() throws Exception {
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
		String relid1 = out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/relationships",createRelation(path3[1],path3[2],"affects",path2[1],path2[2],true).toString());
		assertEquals(201,out.getStatus());	
		String relid2 = out.getHeader("Location");
		log.info("id1="+id1);
		log.info("id2="+id2);
		log.info("id3="+id3);
		log.info("relid1="+relid1);
		log.info("relid2="+relid2);
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
		int i0=0,i1=1;
		String rel_a=rel3.getJSONObject(i0).getString("csid");
		String rel_b=rel3.getJSONObject(i1).getString("csid");
		log.info("rel_a="+rel_a.toString());
		log.info("rel_b="+rel_b.toString());
		if(rel_a.equals(path2[2]) && rel_b.equals(path1[2])) {
			i0=1;
			i1=0;
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
		log.info(out.getContent());
		
		/* clean up */
		
		out=jettyDo(jetty,"DELETE","/chain"+id1,null);
		out=jettyDo(jetty,"DELETE","/chain"+id2,null);
		out=jettyDo(jetty,"DELETE","/chain"+id3,null);
	}
	
	
	@Test public void testLoginTest() throws Exception {
		ServletTester jetty=setupJetty();
		//initially set up with logged in user
		HttpTester out=jettyDo(jetty,"GET","/chain/loginstatus",null);
		JSONObject data3=new JSONObject(out.getContent());
		Boolean rel3=data3.getBoolean("login");
		assertTrue(rel3);
		//logout the user
		out=jettyDo(jetty,"GET","/chain/logout",null);
		//should get false
		out=jettyDo(jetty,"GET","/chain/loginstatus",null);
		JSONObject data2=new JSONObject(out.getContent());
		Boolean rel2=data2.getBoolean("login");
		assertFalse(rel2);
	}
	
	
	// XXX factor out creation
	@Test public void testRelationsMissingOneWay() throws Exception {
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
	
	@Test public void testMultipleCreate() throws Exception {
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
	
	// XXX update of two-way relations
	// XXX update of one-wayness
	@Test public void testUpdate() throws Exception {
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
		// Create a relation 3 -> 1
		out=jettyDo(jetty,"POST","/chain/relationships",createRelation(path3[1],path3[2],"affects",path1[1],path1[2],true).toString());
		assertEquals(201,out.getStatus());		
		// Get csid
		JSONObject data=new JSONObject(out.getContent());
		String csid1=data.getString("csid");
		assertNotNull(csid1);
		log.info("csid="+csid1);
		// Update it to 2 -> 1
		out=jettyDo(jetty,"PUT","/chain/relationships/"+csid1,createRelation(path2[1],path2[2],"affects",path1[1],path1[2],true).toString());
		assertEquals(200,out.getStatus());
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		JSONObject data1=new JSONObject(out.getContent());
		JSONArray rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(0,rel1.length());		
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		JSONObject data2=new JSONObject(out.getContent());
		JSONArray rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(1,rel2.length());		
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		JSONObject data3=new JSONObject(out.getContent());
		JSONArray rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(0,rel3.length());
	}
		
	private void delete_all(ServletTester jetty) throws Exception {
		HttpTester out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		JSONArray items=new JSONObject(out.getContent()).getJSONArray("items");
		for(int i=0;i<items.length();i++) {
			out=jettyDo(jetty,"DELETE","/chain/relationships/one-way/"+items.getString(i),null);
		}
	}
	
	@Test public void testOneWayWorksInUpdate() throws Exception {
		ServletTester jetty=setupJetty();
		delete_all(jetty);
		HttpTester out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("2007.4-a.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/acquisition/",makeSimpleRequest(getResourceString("2005.017.json")));
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		String[] path1=id1.split("/");
		String[] path2=id2.split("/");		
		String[] path3=id3.split("/");
		// Create a relation 3 <-> 1
		out=jettyDo(jetty,"POST","/chain/relationships",createRelation(path3[1],path3[2],"affects",path1[1],path1[2],false).toString());
		assertEquals(201,out.getStatus());		
		// Get csid
		JSONObject data=new JSONObject(out.getContent());
		String csid1=data.getString("csid");
		assertNotNull(csid1);
		log.info("csid="+csid1);
		// Update to 2 <-> 1 keeping one-way false
		out=jettyDo(jetty,"PUT","/chain/relationships/"+csid1,createRelation(path2[1],path2[2],"affects",path1[1],path1[2],false).toString());
		assertEquals(200,out.getStatus());		
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		JSONObject data1=new JSONObject(out.getContent());
		JSONArray rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(1,rel1.length());		
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		JSONObject data2=new JSONObject(out.getContent());
		JSONArray rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(1,rel2.length());		
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		JSONObject data3=new JSONObject(out.getContent());
		JSONArray rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(0,rel3.length());
		// Update to 1 -> 3, making one-way true
		String csid2=rel1.getJSONObject(0).getString("relid");
		out=jettyDo(jetty,"PUT","/chain/relationships/"+csid2,createRelation(path1[1],path1[2],"affects",path3[1],path3[2],true).toString());
		assertEquals(200,out.getStatus());
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		data1=new JSONObject(out.getContent());
		rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(1,rel1.length());		
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		data2=new JSONObject(out.getContent());
		rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(0,rel2.length());		
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		data3=new JSONObject(out.getContent());
		rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(0,rel3.length());
		// Update to 3 -> 1, keeping one way true
		out=jettyDo(jetty,"PUT","/chain/relationships/"+csid2,createRelation(path3[1],path3[2],"affects",path1[1],path1[2],true).toString());
		assertEquals(200,out.getStatus());
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		data1=new JSONObject(out.getContent());
		rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(0,rel1.length());		
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		data2=new JSONObject(out.getContent());
		rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(0,rel2.length());		
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		data3=new JSONObject(out.getContent());
		rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(1,rel3.length());
		// Update to 1 <-> 2, making one way false
		out=jettyDo(jetty,"PUT","/chain/relationships/"+csid2,createRelation(path1[1],path1[2],"affects",path2[1],path2[2],false).toString());
		assertEquals(200,out.getStatus());
		// Check it
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		data1=new JSONObject(out.getContent());
		rel1=data1.getJSONArray("relations");
		assertNotNull(rel1);
		assertEquals(1,rel1.length());		
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		data2=new JSONObject(out.getContent());
		rel2=data2.getJSONArray("relations");
		assertNotNull(rel2);
		assertEquals(1,rel2.length());		
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		data3=new JSONObject(out.getContent());
		rel3=data3.getJSONArray("relations");
		assertNotNull(rel3);
		assertEquals(0,rel3.length());		
		
		
	}
	
	@Test public void testRelationshipType() throws Exception {
		// Create test objects
		ServletTester jetty=setupJetty();
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("2007.4-a.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		String[] path1=id1.split("/");
		String[] path2=id2.split("/");
		// Relate them
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path2[1],path2[2],"affects",path1[1],path1[2],false).toString());
		assertEquals(201,out.getStatus());	
		// Check types
		out=jettyDo(jetty,"GET","/chain"+id1,null);
		JSONObject data1=new JSONObject(out.getContent());
		JSONArray rels1=data1.getJSONArray("relations");
		assertNotNull(rels1);
		assertEquals(1,rels1.length());		
		JSONObject rel1=rels1.getJSONObject(0);
		assertEquals(rel1.getString("recordtype"),"intake");
		out=jettyDo(jetty,"GET","/chain"+id2,null);
		JSONObject data2=new JSONObject(out.getContent());
		JSONArray rels2=data2.getJSONArray("relations");
		assertNotNull(rels2);
		assertEquals(1,rels2.length());		
		JSONObject rel2=rels2.getJSONObject(0);
		assertEquals(rel2.getString("recordtype"),"objects");
	}
	
	@Test public void testSearchList() throws Exception {
		ServletTester jetty=setupJetty();
		delete_all(jetty);
		// Check list is empty
		HttpTester out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		JSONArray items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(0,items.length());
		// Create some objects
		out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("2007.4-a.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/acquisition/",makeSimpleRequest(getResourceString("2005.017.json")));
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		String[] path1=id1.split("/");
		String[] path2=id2.split("/");		
		String[] path3=id3.split("/");
		// Add a relation rel1: 2 -> 1
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path2[1],path2[2],"affects",path1[1],path1[2],true).toString());
		assertEquals(201,out.getStatus());	
		// Check length is 1 and it points to a valid and correct relation
		out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());
		String rel1_csid=items.getString(0);
		assertNotNull(rel1_csid);
		out=jettyDo(jetty,"GET","/chain/relationships/"+rel1_csid,null);
		assertEquals(200,out.getStatus());
		JSONObject rel1=new JSONObject(out.getContent());
		assertEquals(path2[2],rel1.getJSONObject("source").getString("csid"));
		assertEquals(path1[2],rel1.getJSONObject("target").getString("csid"));
		// Add some more relations: rel2: 2 -> 3 ; rel 3: 3 -> 1 (new type)
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path2[1],path2[2],"affects",path3[1],path3[2],true).toString());
		assertEquals(201,out.getStatus());	
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path3[1],path3[2],"new",path1[1],path1[2],true).toString());
		assertEquals(201,out.getStatus());	
		// Total length should be 3
		out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(3,items.length());
		// Should be two starting at 2
		out=jettyDo(jetty,"GET","/chain/relationships/search?source="+path2[1]+"/"+path2[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(2,items.length());		
		// Should be one staring at 3, none at 1
		out=jettyDo(jetty,"GET","/chain/relationships/search?source="+path3[1]+"/"+path3[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());		
		out=jettyDo(jetty,"GET","/chain/relationships/search?source="+path1[1]+"/"+path1[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(0,items.length());		
		// Targets: two at 1, none at 2, one at 3
		out=jettyDo(jetty,"GET","/chain/relationships/search?target="+path1[1]+"/"+path1[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(2,items.length());
		out=jettyDo(jetty,"GET","/chain/relationships/search?target="+path2[1]+"/"+path2[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(0,items.length());		
		out=jettyDo(jetty,"GET","/chain/relationships/search?target="+path3[1]+"/"+path3[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());		
		// Type, two "affects", one "new"
		out=jettyDo(jetty,"GET","/chain/relationships/search?type=affects",null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(2,items.length());
		out=jettyDo(jetty,"GET","/chain/relationships/search?type=new",null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());	
		// Combination: target = 1, type = affects; just one
		out=jettyDo(jetty,"GET","/chain/relationships/search?type=affects&target="+path1[1]+"/"+path1[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());
		// Combination: source = 2, target = 3; just one
		out=jettyDo(jetty,"GET","/chain/relationships/search?source="+path2[1]+"/"+path2[2]+"&target="+path3[1]+"/"+path3[2],null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());
	}
	
	@Test public void testDelete() throws Exception {
		ServletTester jetty=setupJetty();
		delete_all(jetty);
		// Create some objects
		HttpTester out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("2007.4-a.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/acquisition/",makeSimpleRequest(getResourceString("2005.017.json")));
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		String[] path1=id1.split("/");
		String[] path2=id2.split("/");		
		String[] path3=id3.split("/");
		// Create two relationships, one two way
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path2[1],path2[2],"affects",path1[1],path1[2],true).toString());
		assertEquals(201,out.getStatus());	
		out=jettyDo(jetty,"POST","/chain/relationships/",createRelation(path3[1],path3[2],"affects",path1[1],path1[2],false).toString());
		assertEquals(201,out.getStatus());	
		String csid=new JSONObject(out.getContent()).getString("csid");
		// Check length is 3
		out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		JSONArray items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(3,items.length());
		// Delete the two way relationship
		out=jettyDo(jetty,"DELETE","/chain/relationships/"+csid,null);
		assertEquals(200,out.getStatus());
		// Check length is 1, and it's the right one
		out=jettyDo(jetty,"GET","/chain/relationships/",null);
		assertEquals(200,out.getStatus());
		items=new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1,items.length());
		out=jettyDo(jetty,"GET","/chain/relationships/"+items.getString(0),null);
		JSONObject rel1=new JSONObject(out.getContent());
		assertEquals(path2[2],rel1.getJSONObject("source").getString("csid"));
		assertEquals(path1[2],rel1.getJSONObject("target").getString("csid"));		
	}
	// XXX DELETE RELATIONS WHEN RECORD IS DELETED: NOT FOR 0.5
}
