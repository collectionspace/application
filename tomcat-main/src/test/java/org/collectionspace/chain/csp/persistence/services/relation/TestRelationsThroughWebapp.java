/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.relation;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRelationsThroughWebapp {
	private static final Logger log = LoggerFactory
			.getLogger(TestRelationsThroughWebapp.class);
	private static TestBase tester = new TestBase();
	

	private JSONObject createMini(String type, String id) throws JSONException {
		JSONObject out = new JSONObject();
		out.put("csid", id);
		out.put("recordtype", type);
		return out;
	}

	private JSONObject createRelation(String src_type, String src, String type,
			String dst_type, String dst, boolean one_way) throws JSONException {
		JSONObject out = new JSONObject();
		out.put("source", createMini(src_type, src));
		out.put("target", createMini(dst_type, dst));
		out.put("type", type);
		out.put("one-way", one_way);
		return out;
	}
	


	@Test
	public void testRelationsCreate() throws Exception {
		// First create atester. couple of cataloging
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id1 = out.getHeader("Location");
		out =tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		// Now create a pair of relations in: 3<->1, 3->2: a. 1->3, b. 3->1, c.
		// 3->2
		out = tester.POSTData("/relationships", createRelation(path3[1], path3[2],
				"affects", path1[1], path1[2], false), jetty);
		log.info(out.getContent());
		String relid1 = out.getHeader("Location");
		String csid1 = new JSONObject(out.getContent()).getString("csid");

		out = tester.POSTData("/relationships", createRelation(path3[1], path3[2],
				"affects", path2[1], path2[2], true), jetty);
		log.info(out.getContent());
		String relid2 = out.getHeader("Location");
		String csid2 = new JSONObject(out.getContent()).getString("csid");

		// Check 1 has relation to 3
		out = tester.GETData(id1, jetty);
		JSONObject data1 = new JSONObject(out.getContent());
		// that the destination is 3
		log.info(out.getContent());
		JSONArray rel1 = data1.getJSONObject("relations").getJSONArray("cataloging");
		assertNotNull(rel1);
		assertEquals(1, rel1.length());
		JSONObject mini1 = rel1.getJSONObject(0);
		assertEquals("cataloging", mini1.getString("recordtype"));
		assertEquals(mini1.getString("csid"), path3[2]);
		/*
		 * relid and relationshiptype are no longer provided in the relation payload
		String rida = mini1.getString("relid");

		// pull the relation itself, and check it
		out = tester.GETData("/relationships/" + rida, jetty);
		JSONObject rd1 = new JSONObject(out.getContent());
		assertEquals("affects", rd1.getString("type"));
		assertEquals(rida, rd1.getString("csid"));
		JSONObject src1 = rd1.getJSONObject("source");
		assertEquals("cataloging", src1.getString("recordtype"));
		assertEquals(path1[2], src1.get("csid"));
		JSONObject dst1 = rd1.getJSONObject("target");
		assertEquals("cataloging", dst1.getString("recordtype"));
		assertEquals(path3[2], dst1.get("csid"));
		*/

		// Check that 2 has no relations at all
		out = tester.GETData(id2, jetty);
		JSONObject data2 = new JSONObject(out.getContent());
		// that the destination is 3
		JSONObject rel2 = data2.getJSONObject("relations");
		assertNotNull(rel2);
		assertEquals(0, rel2.length());
		// Check that 3 has relations to 1 and 2
		out = tester.GETData(id3, jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		// untangle them
		JSONArray rel3 = data3.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rel3);
		assertEquals(2, rel3.length());
		int i0 = 0, i1 = 1;
		String rel_a = rel3.getJSONObject(i0).getString("csid");
		String rel_b = rel3.getJSONObject(i1).getString("csid");

		if (rel_a.equals(path2[2]) && rel_b.equals(path1[2])) {
			i0 = 1;
			i1 = 0;
		}
		JSONObject rel31 = rel3.getJSONObject(i0);
		JSONObject rel32 = rel3.getJSONObject(i1);
		// check desintations
		assertEquals("cataloging", rel31.getString("recordtype"));
		assertEquals(rel31.getString("csid"), path1[2]);
		/*
		 * relid no longer provided in the payloads
		 */
		//String rid31 = rel31.getString("relid");
		assertEquals("cataloging", rel32.getString("recordtype"));
		assertEquals(rel32.getString("csid"), path2[2]);
		//String rid32 = rel32.getString("relid");
		// check actual records
		// 3 -> 1
		// out = tester.GETData("/relationships/" + rid31, jetty);
		// JSONObject rd31 = new JSONObject(out.getContent());
		// assertEquals("affects", rd31.getString("type"));
		// assertEquals(rid31, rd31.getString("csid"));
		// JSONObject src31 = rd31.getJSONObject("source");
		// assertEquals("cataloging", src31.getString("recordtype"));
		// assertEquals(path3[2], src31.get("csid"));
		// JSONObject dst31 = rd31.getJSONObject("target");
		// assertEquals("cataloging", dst31.getString("recordtype"));
		// assertEquals(path1[2], dst31.get("csid"));
		// 3 -> 2
		// out = tester.GETData("/relationships/" + rid32, jetty);
		// JSONObject rd32 = new JSONObject(out.getContent());
		// assertEquals("affects", rd32.getString("type"));
		// assertEquals(rid32, rd32.getString("csid"));
		// JSONObject src32 = rd32.getJSONObject("source");
		// assertEquals("cataloging", src32.getString("recordtype"));
		// assertEquals(path3[2], src32.get("csid"));
		// JSONObject dst32 = rd32.getJSONObject("target");
		// assertEquals("cataloging", dst32.getString("recordtype"));
		// assertEquals(path2[2], dst32.get("csid"));

		/* clean up */
		tester.DELETEData("/relationships/" + csid1, jetty);
		tester.DELETEData("/relationships/" + csid2, jetty);
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		tester.stopJetty(jetty);
	}

	@Test
	public void testLoginTest() throws Exception {
		// initially set up with logged in user
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.GETData("/loginstatus", jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		Boolean rel3 = data3.getBoolean("login");
		assertTrue(rel3);
		// logout the user
		out = tester.GETData("/logout", jetty, 303);
		// should get false
		out = tester.GETData("/loginstatus", jetty);
		JSONObject data2 = new JSONObject(out.getContent());
		Boolean rel2 = data2.getBoolean("login");
		assertFalse(rel2);
		tester.stopJetty(jetty);
	}

	// XXX factor out creation
	@Test
	public void testRelationsMissingOneWay() throws Exception {
		// First create a couple of cataloging
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);

		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path3 = id3.split("/");
		JSONObject data = createRelation(path3[1], path3[2], "affects",
				path1[1], path1[2], false);
		data.remove("one-way");
		out = tester.POSTData("/relationships", data, jetty);
		// Get csid
		JSONObject datacs = new JSONObject(out.getContent());
		String csid1 = datacs.getString("csid");
		// Just heck they have length 1 (other stuff will be tested by main
		// test)
		out = tester.GETData(id3, jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		JSONArray rel3 = data3.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rel3);
		assertEquals(1, rel3.length());
		out = tester.GETData(id1, jetty);
		JSONObject data1 = new JSONObject(out.getContent());
		JSONArray rel1 = data1.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rel1);
		assertEquals(1, rel1.length());

		// clean up after
		tester.DELETEData("/relationships/" + csid1, jetty);
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		tester.stopJetty(jetty);
	}

	@Test
	public void testMultipleCreate() throws Exception {
		// Create test cataloging
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		// Do the rleation
		JSONObject data1 = createRelation(path3[1], path3[2], "affects",
				path1[1], path1[2], false);
		JSONObject data2 = createRelation(path3[1], path3[2], "affects",
				path2[1], path2[2], false);
		JSONArray datas = new JSONArray();
		datas.put(data1);
		datas.put(data2);
		JSONObject data = new JSONObject();
		data.put("items", datas);
		out = tester.POSTData("/relationships", data, jetty);
		// Check it
		out = tester.GETData(id3, jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		JSONArray rel3 = data3.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rel3);
		assertEquals(2, rel3.length());

		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		tester.stopJetty(jetty);
	}

	// XXX update of two-way relations
	// XXX update of one-wayness
	@Test
	public void testUpdate() throws Exception {
		// Create test cataloging
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		
		// Create a relation 3 -> 1
		out = tester.POSTData("/relationships", createRelation(path3[1], path3[2],
				"affects", path1[1], path1[2], true), jetty);
		// Get csid
		JSONObject data = new JSONObject(out.getContent());
		log.info(out.getContent());
		String csid1 = data.getString("csid");
		
		assertNotNull(csid1);
		// Update it to 2 -> 1
		out = tester.PUTData("/relationships/" + csid1, createRelation(path2[1],
				path2[2], "affects", path1[1], path1[2], true), jetty);

		log.info(out.getContent());
		// Check it
		
		out = tester.GETData(id1, jetty);
		JSONObject data1 = new JSONObject(out.getContent());
		JSONObject rel1 = data1.getJSONObject("relations");
		
		out = tester.GETData(id2, jetty);
		JSONObject data2 = new JSONObject(out.getContent());
		log.info(out.getContent());
		JSONArray rel2 = data2.getJSONObject("relations").getJSONArray(
				"cataloging");
		
		out = tester.GETData(id3, jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		JSONObject rel3 = data3.getJSONObject("relations");
		
		// clean up after
		tester.DELETEData("/relationships/" + csid1, jetty);

		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		
//test
		assertNotNull(rel1);
		assertEquals(0, rel1.length());
		assertNotNull(rel2);
		assertEquals(1, rel2.length());
		assertNotNull(rel3);
		assertEquals(0, rel3.length());
		tester.stopJetty(jetty);
	}

	@Test
	public void testOneWayWorksInUpdate() throws Exception {
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/intake/",
				tester.makeSimpleRequest(tester.getResourceString("2007.4-a.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/acquisition/",
				tester.makeSimpleRequest(tester.getResourceString("2005.017.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		// Create a relation 3 <-> 1
		out = tester.POSTData("/relationships", createRelation(path3[1], path3[2],
				"affects", path1[1], path1[2], false), jetty);
		// Get csid
		JSONObject data = new JSONObject(out.getContent());
		String csid1 = data.getString("csid");

		assertNotNull(csid1);
		// Update to 2 <-> 1 keeping one-way false
		out = tester.PUTData("/relationships/" + csid1, createRelation(path2[1],
				path2[2], "affects", path1[1], path1[2], false), jetty);

		// Check it
		out = tester.GETData(id1, jetty);
		JSONObject data1 = new JSONObject(out.getContent());
		JSONArray rel1 = data1.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rel1);
		assertEquals(1, rel1.length());
		out = tester.GETData(id2, jetty);
		JSONObject data2 = new JSONObject(out.getContent());
		JSONArray rel2 = data2.getJSONObject("relations").getJSONArray("intake");

		assertNotNull(rel2);
		assertEquals(1, rel2.length());
		out = tester.GETData(id3, jetty);
		JSONObject data3 = new JSONObject(out.getContent());
		JSONObject rel3 = data3.getJSONObject("relations");
		assertNotNull(rel3);
		assertEquals(0, rel3.length());
		// Update to 1 -> 3, making one-way true
		//String csid2 = rel1.getJSONObject(0).getString("relid");
		out = tester.PUTData("/relationships/" + csid1, createRelation(path1[1],
				path1[2], "affects", path3[1], path3[2], true), jetty);

		// Check it
		out = tester.GETData(id1, jetty);
		data1 = new JSONObject(out.getContent());
		rel1 = data1.getJSONObject("relations").getJSONArray("acquisition");
		assertNotNull(rel1);
		assertEquals(1, rel1.length());
		out = tester.GETData(id2, jetty);
		data2 = new JSONObject(out.getContent());
		JSONObject rel2a = data2.getJSONObject("relations");
		assertNotNull(rel2a);
		assertEquals(0, rel2a.length());
		out = tester.GETData(id3, jetty);
		data3 = new JSONObject(out.getContent());
		rel3 = data3.getJSONObject("relations");
		assertNotNull(rel3);
		assertEquals(0, rel3.length());
		// Update to 3 -> 1, keeping one way true
		out = tester.PUTData("/relationships/" + csid1, createRelation(path3[1],
				path3[2], "affects", path1[1], path1[2], true), jetty);

		// Check it
		out = tester.GETData(id1, jetty);
		data1 = new JSONObject(out.getContent());
		JSONObject rel1a = data1.getJSONObject("relations");
		assertNotNull(rel1a);
		assertEquals(0, rel1a.length());
		out = tester.GETData(id2, jetty);
		data2 = new JSONObject(out.getContent());
		rel2a = data2.getJSONObject("relations");
		assertNotNull(rel2a);
		assertEquals(0, rel2a.length());
		out = tester.GETData(id3, jetty);
		data3 = new JSONObject(out.getContent());
		JSONArray rel3a = data3.getJSONObject("relations").getJSONArray(
				"intake");
		assertNotNull(rel3a);
		assertEquals(1, rel3a.length());
		// Update to 1 <-> 2, making one way false
		out = tester.PUTData("/relationships/" + csid1, createRelation(path1[1],
				path1[2], "affects", path2[1], path2[2], false), jetty);

		// Check it
		out = tester.GETData(id1, jetty);
		data1 = new JSONObject(out.getContent());
		rel1 = data1.getJSONObject("relations").getJSONArray("cataloging");
		assertNotNull(rel1);
		assertEquals(1, rel1.length());
		out = tester.GETData(id2, jetty);
		data2 = new JSONObject(out.getContent());
		rel2 = data2.getJSONObject("relations").getJSONArray("intake");
		assertNotNull(rel2);
		assertEquals(1, rel2.length());
		out = tester.GETData(id3, jetty);
		data3 = new JSONObject(out.getContent());
		rel3 = data3.getJSONObject("relations");
		assertNotNull(rel3);
		assertEquals(0, rel3.length());

		// clean up after
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);

		tester.stopJetty(jetty);
	}

	@Test
	public void testRelationshipType() throws Exception {
		// Create test cataloging
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/intake/",
				tester.makeSimpleRequest(tester.getResourceString("2007.4-a.json")), jetty);
		String id2 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		// Relate them
		out = tester.POSTData("/relationships/", createRelation(path2[1], path2[2],
				"affects", path1[1], path1[2], false), jetty);
		String csid = new JSONObject(out.getContent()).getString("csid");
		// Check types
		out = tester.GETData(id1, jetty);
		JSONObject data1 = new JSONObject(out.getContent());
		JSONArray rels1 = data1.getJSONObject("relations").getJSONArray(
				"intake");
		assertNotNull(rels1);
		assertEquals(1, rels1.length());
		JSONObject rel1 = rels1.getJSONObject(0);
		assertEquals(rel1.getString("recordtype"), "intake");
		out = tester.GETData(id2, jetty);
		JSONObject data2 = new JSONObject(out.getContent());
		JSONArray rels2 = data2.getJSONObject("relations").getJSONArray(
				"cataloging");
		assertNotNull(rels2);
		assertEquals(1, rels2.length());
		JSONObject rel2 = rels2.getJSONObject(0);
		assertEquals(rel2.getString("recordtype"), "cataloging");

		// clean up after
		tester.DELETEData("/relationships/" + csid, jetty);
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.stopJetty(jetty);

	}
	/* this is not testing anything - make it test something
	@Test 
	public void testHierarchical() throws Exception{
		// Check list is empty;

		ServletTester jetty = tester.setupJetty();
		String st = "/relationships/hierarchical/search?source=person/a93233e6-ca44-477d-97a0&type=hasBroader";
		HttpTester out = tester.GETData(st, jetty);
		log.info(out.getContent());
		tester.stopJetty(jetty);
	}
	*/

	@Test
	public void testSearchList() throws Exception {
		// Check list is empty
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.GETData("/relationships/", jetty);
		JSONArray items = new JSONObject(out.getContent())
				.getJSONArray("items");
		Integer offset = items.length();
		// assertEquals(0,items.length());
		// Create some cataloging
		out = tester.POSTData("/intake/",
				tester.makeSimpleRequest(tester.getResourceString("2007.4-a.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/acquisition/",
				tester.makeSimpleRequest(tester.getResourceString("2005.017.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		// Add a relation rel1: 2 -> 1
		out = tester.POSTData("/relationships/", createRelation(path2[1], path2[2],
				"affects", path1[1], path1[2], true), jetty);
		String csid1 = new JSONObject(out.getContent()).getString("csid");

		out = tester.GETData("/relationships/" + csid1, jetty);
		JSONObject rel1 = new JSONObject(out.getContent());
		assertEquals(path2[2], rel1.getJSONObject("source").getString("csid"));
		assertEquals(path1[2], rel1.getJSONObject("target").getString("csid"));
		// Add some more relations: rel2: 2 -> 3 ; rel 3: 3 -> 1 (new type)
		out = tester.POSTData("/relationships/", createRelation(path2[1], path2[2],
				"affects", path3[1], path3[2], true), jetty);
		String csid2 = new JSONObject(out.getContent()).getString("csid");
		out = tester.POSTData("/relationships/", createRelation(path3[1], path3[2],
				"broader", path1[1], path1[2], true), jetty);
		String csid3 = new JSONObject(out.getContent()).getString("csid");
		// Total length should be 3 XXX pagination & offset
		// out = GETData("/relationships",jetty);
		// items=new JSONObject(out.getContent()).getJSONArray("items");
		// assertEquals(3,items.length());
		// Should be two starting at 2
		out = tester.GETData("/relationships/search?source=" + path2[1] + "/"
				+ path2[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(2, items.length());
		// Should be one staring at 3, none at 1
		out = tester.GETData("/relationships/search?source=" + path3[1] + "/"
				+ path3[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1, items.length());
		out = tester.GETData("/relationships/search?source=" + path1[1] + "/"
				+ path1[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(0, items.length());
		// Targets: two at 1, none at 2, one at 3
		out = tester.GETData("/relationships/search?target=" + path1[1] + "/"
				+ path1[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(2, items.length());
		out = tester.GETData("/relationships/search?target=" + path2[1] + "/"
				+ path2[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(0, items.length());
		out = tester.GETData("/relationships/search?target=" + path3[1] + "/"
				+ path3[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1, items.length());

		// out=GETData("/relationships/search?type=broader",null);
		// items=new JSONObject(out.getContent()).getJSONArray("items");
		// assertEquals(1,items.length());
		// Combination: target = 1, type = affects; just one
		out = tester.GETData("/relationships/search?type=affects&target=" + path1[1]
				+ "/" + path1[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1, items.length());
		// Combination: source = 2, target = 3; just one
		out = tester.GETData("/relationships/search?source=" + path2[1] + "/"
				+ path2[2] + "&target=" + path3[1] + "/" + path3[2], jetty);
		items = new JSONObject(out.getContent()).getJSONArray("items");
		assertEquals(1, items.length());

		// clean up after
		tester.DELETEData("/relationships/" + csid1, jetty);
		tester.DELETEData("/relationships/" + csid2, jetty);
		tester.DELETEData("/relationships/" + csid3, jetty);
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		tester.stopJetty(jetty);
	}

	@Test
	public void testDelete() throws Exception {
		// Check size of initial is empty
		ServletTester jetty = tester.setupJetty();
		HttpTester out = tester.GETData("/relationships/", jetty);
		JSONArray itemsall = new JSONObject(out.getContent())
				.getJSONArray("items");
		Integer offset = itemsall.length();

		// Create some cataloging
		out = tester.POSTData("/intake/",
				tester.makeSimpleRequest(tester.getResourceString("2007.4-a.json")), jetty);
		String id1 = out.getHeader("Location");
		out = tester.POSTData("/cataloging/",
				tester.makeSimpleRequest(tester.getResourceString("obj3.json")), jetty);
		String id2 = out.getHeader("Location");
		out = tester.POSTData("/acquisition/",
				tester.makeSimpleRequest(tester.getResourceString("2005.017.json")), jetty);
		String id3 = out.getHeader("Location");
		String[] path1 = id1.split("/");
		String[] path2 = id2.split("/");
		String[] path3 = id3.split("/");
		// Create three relationships, one two way
		out = tester.POSTData("/relationships/", createRelation(path2[1], path2[2],
				"affects", path1[1], path1[2], true), jetty);
		String csid2 = new JSONObject(out.getContent()).getString("csid");
		out = tester.POSTData("/relationships/", createRelation(path3[1], path3[2],
				"affects", path1[1], path1[2], false), jetty);
		String csid = new JSONObject(out.getContent()).getString("csid");

		out = tester.POSTData("/relationships/", createRelation(path3[1], path3[2],
				"affects", path2[1], path2[2], false), jetty);
		String csid3 = new JSONObject(out.getContent()).getString("csid");
		
		//delete first relationship
		tester.DELETEData("/relationships/" + csid2, jetty);
		
		//delete second relationship
		String path = "/relationships/0?source="+id3+"&target="+id1+"&type=affects";
		tester.DELETEData(path, jetty);
		
		//delete third relationship
		JSONObject delrel = new JSONObject();
		JSONObject source = new JSONObject();
		JSONObject target = new JSONObject();
		source.put("csid", path3[2]);
		source.put("recordtype", path3[1]);
		target.put("csid", path2[2]);
		target.put("recordtype", path2[1]);
		delrel.put("source", source);
		delrel.put("target", target);
		delrel.put("type", "affects");
		delrel.put("one-way", "false");
		tester.DELETEData("/relationships/0", jetty, delrel.toString());
		
		
		
		tester.DELETEData(id1, jetty);
		tester.DELETEData(id2, jetty);
		tester.DELETEData(id3, jetty);
		tester.stopJetty(jetty);
	}
	// XXX DELETE RELATIONS WHEN RECORD IS DELETED: NOT FOR 0.5
}
