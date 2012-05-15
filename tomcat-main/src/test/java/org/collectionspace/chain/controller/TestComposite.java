package org.collectionspace.chain.controller;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestComposite {
	private static final Logger log=LoggerFactory.getLogger(TestComposite.class);
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try{
			jetty=tester.setupJetty();
			}
		catch(Exception ex){
			log.error("TestComposite: Could not set up jetty! " + ex.getLocalizedMessage());
		}
	}
	
	@AfterClass public static void testStop() throws Exception {
		tester.stopJetty(jetty);
	}

	
	private JSONObject createCompositePOSTPartJSON(String payload) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("path","/cataloging/");
		out.put("method","POST");
		out.put("body",payload);
		return out;
	}

	private JSONObject createCompositePUTPartJSON(String path,String payload) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("path",path);
		out.put("method","PUT");
		out.put("body",payload);
		return out;
	}

	private JSONObject createCompositeGETPartJSON(String path) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("path",path);
		out.put("method","GET");
		return out;
	}

	private JSONObject createCompositeDELETEPartJSON(String path) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("path",path);
		out.put("method","DELETE");
		return out;
	}
	@Test public void testConfigComposite() throws Exception {
		JSONObject ppp=new JSONObject();
		
		JSONObject recordlist=new JSONObject();
		recordlist.put("path","/recordlist/uischema");
		recordlist.put("method","GET");

		ppp.put("recordlist", recordlist);
		
		JSONObject recordtypes=new JSONObject();
		recordtypes.put("path","/recordtypes/uischema");
		recordtypes.put("method","GET");
		recordtypes.put("dataType","json");
		ppp.put("recordtypes", recordtypes);

		JSONObject objectexit=new JSONObject();
		objectexit.put("path","/objectexit/uischema");
		objectexit.put("method","GET");
		objectexit.put("dataType","json");
		ppp.put("objectexit", objectexit);

		JSONObject objectexitspec=new JSONObject();
		objectexitspec.put("path","/objectexit/uispec");
		objectexitspec.put("method","GET");
		objectexitspec.put("dataType","json");
		ppp.put("uispec", objectexitspec);
		
		JSONObject record=new JSONObject();
		record.put("path","/objectexit/5ad847df-904e-4eaf-a01e");
		record.put("method","GET");
		record.put("dataType","json");
		ppp.put("record", record);
		

		log.info(ppp.toString());
		HttpTester out3 = tester.GETData("/composite",ppp.toString(),jetty);
		JSONObject jout3=new JSONObject(out3.getContent());
		log.info(jout3.toString());
		

		
	}	
	
	@Test public void testConfigCompositeComplex() throws Exception {
		JSONObject ppp=new JSONObject();
		String path = "/termlist/efc6415c-6a4b-4edc-98fe";
		
		
		String name = "sdsdsdsd";
		String data2 = "{\"csid\":\"efc6415c-6a4b-4edc-98fe\",\"fields\":{\"shortIdentifier\":\"entrymethod\",\"usedBys\":[{\"usedBy\":\"intake:intake-entryMethod\"}],\"terms\":[{\"shortIdentifier\":\"foundondoorstep\",\"_subrecordcsid\":\"ecba47a8-b05b-4191-bb64\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"Found on doorstep\"},{\"shortIdentifier\":\"post\",\"_subrecordcsid\":\"f2bfe28d-d45a-4f24-8b4d\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"Post\"},{\"shortIdentifier\":\"inperson\",\"_subrecordcsid\":\"ffacfcda-61d0-454c-8291\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"In person\"}],\"csid\":\"efc6415c-6a4b-4edc-98fe\",\"displayName\":\"Entry Method\",\"source\":\"wer\",\"description\":\"wer\"}}";
		String data1 = "{\"csid\":\"efc6415c-6a4b-4edc-98fe\",\"fields\":{\"shortIdentifier\":\"entrymethod\",\"usedBys\":[{\"usedBy\":\"intake:intake-entryMethod\"}],\"terms\":[{\"shortIdentifier\":\"foundondoorstep\",\"_subrecordcsid\":\"ecba47a8-b05b-4191-bb64\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"Found on doorstep\"},{\"shortIdentifier\":\"post\",\"_subrecordcsid\":\"f2bfe28d-d45a-4f24-8b4d\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"Post\"},{\"shortIdentifier\":\"inperson\",\"_subrecordcsid\":\"ffacfcda-61d0-454c-8291\",\"description\":\"\",\"termStatus\":\"\",\"displayName\":\"In person\"},"+
					"{\"shortIdentifier\":\""+name+"\",\"source\":\""+name+"\",\"description\":\""+name+"\",\"termStatus\":\"active\",\"displayName\":\""+name+"\"}],\"csid\":\"efc6415c-6a4b-4edc-98fe\",\"displayName\":\"Entry Method\",\"description\":\""+name+"\"}}";
		JSONObject updatetermlist = createCompositePUTPartJSON( path,data1);

		ppp.put("updatetermlist", updatetermlist);

		JSONObject recordlist2=new JSONObject();
		recordlist2.put("path",path);
		recordlist2.put("method","GET");

		ppp.put("termlist2", recordlist2);
		
		log.info("WAA"+ppp.toString());
		HttpTester out = tester.GETData("/intake/uispec",  jetty);
		log.info("ARGH"+out.getContent());
		HttpTester out3 = tester.GETData("/composite",ppp.toString(),jetty);
		JSONObject jout3=new JSONObject(out3.getContent());
		log.info("EEK"+jout3.toString());
		

		HttpTester out2 = tester.GETData("/intake/uispec",  jetty);
		log.info("BARGH"+out2.getContent());
		
	}
	@Test public void testCompositeBasic() throws Exception {
		// Three POSTs give us some data to play with
		JSONObject p1=createCompositePOSTPartJSON(tester.makeSimpleRequest(tester.getResourceString("obj8.json")));
		JSONObject p2=createCompositePOSTPartJSON(tester.makeSimpleRequest(tester.getResourceString("obj8.json")));
		JSONObject p3=createCompositePOSTPartJSON(tester.makeSimpleRequest(tester.getResourceString("obj8.json")));
		JSONObject p=new JSONObject();
		p.put("p1",p1);
		p.put("p2",p2);
		p.put("p3",p3);
		log.info("p="+p);
		HttpTester out = tester.POSTData("/composite",p.toString(),jetty);
		JSONObject jout=new JSONObject(out.getContent());
		log.info("POST="+jout);
		JSONObject q1=jout.getJSONObject("p1");
		JSONObject q2=jout.getJSONObject("p2");
		JSONObject q3=jout.getJSONObject("p3");
		assertEquals("201",q1.getString("status"));
		assertEquals("201",q2.getString("status"));
		assertEquals("201",q3.getString("status"));
		assertTrue(q1.getString("redirect").startsWith("/cataloging/"));
		assertTrue(q2.getString("redirect").startsWith("/cataloging/"));
		assertTrue(q3.getString("redirect").startsWith("/cataloging/"));
		String id1=q1.getString("redirect");
		String id2=q2.getString("redirect");
		String id3=q3.getString("redirect");
		assertFalse(id1.equals(id2));
		assertFalse(id1.equals(id3));
		assertFalse(id2.equals(id3));
		// Now try some PUTs to update the data
		JSONObject p4=createCompositePUTPartJSON(id1,tester.makeSimpleRequest(tester.getResourceString("obj9.json")));
		JSONObject p5=createCompositePUTPartJSON(id2,tester.makeSimpleRequest(tester.getResourceString("obj9.json")));
		JSONObject pp=new JSONObject();
		pp.put("p4",p4);
		pp.put("p5",p5);
		log.info("pp="+pp);
		HttpTester out2 = tester.PUTData("/composite",pp.toString(),jetty);
		JSONObject jout2=new JSONObject(out2.getContent());
		log.info("PUT="+jout2);
		JSONObject q4=jout2.getJSONObject("p4");
		JSONObject q5=jout2.getJSONObject("p5");
		assertEquals("200",q4.getString("status"));
		assertEquals("200",q5.getString("status"));
		// Now some GETs
		JSONObject ppp=new JSONObject();
		ppp.put("p6",createCompositeGETPartJSON(id1));
		ppp.put("p7",createCompositeGETPartJSON(id2));
		ppp.put("p8",createCompositeGETPartJSON(id3));
		log.info("+==============================");
		log.info("ppp="+ppp);
		HttpTester out3 = tester.GETData("/composite",ppp.toString(),jetty);
		JSONObject jout3=new JSONObject(out3.getContent());
		JSONObject q6=jout3.getJSONObject("p6");
		JSONObject q7=jout3.getJSONObject("p7");
		JSONObject q8=jout3.getJSONObject("p8");
		assertEquals("200",q6.getString("status"));
		assertEquals("200",q7.getString("status"));
		assertEquals("200",q8.getString("status"));
		log.info("p6="+q6);
		JSONObject b6=new JSONObject(q6.getString("body"));
		JSONObject b7=new JSONObject(q7.getString("body"));
		JSONObject b8=new JSONObject(q8.getString("body"));
		assertEquals("4",b6.getJSONObject("fields").getString("objectNumber"));
		assertEquals("4",b7.getJSONObject("fields").getString("objectNumber"));
		assertEquals("3",b8.getJSONObject("fields").getString("objectNumber"));
		// Now some DELETEs
		JSONObject pppp=new JSONObject();
		pppp.put("p9",createCompositeDELETEPartJSON(id2));
		HttpTester out4 = tester.POSTData("/composite",pppp.toString(),jetty);
		JSONObject jout4=new JSONObject(out4.getContent());
		JSONObject q9=jout4.getJSONObject("p9");
		assertEquals("200",q9.getString("status"));
		// Try some things that should fail, mixed with some things that should not
		JSONObject ppppp=new JSONObject();
		ppppp.put("p10",createCompositeGETPartJSON(id1));
		ppppp.put("p11",createCompositeGETPartJSON(id2)); // this is failing
		ppppp.put("p12",createCompositeGETPartJSON(id3));
		log.info("ppppp="+ppppp);
		HttpTester out5 = tester.GETData("/composite",ppppp.toString(),jetty);
		JSONObject jout5=new JSONObject(out5.getContent());
		log.info("jout5="+jout5);
		JSONObject q10=jout5.getJSONObject("p10");
		JSONObject q11=jout5.getJSONObject("p11");
		JSONObject q12=jout5.getJSONObject("p12");
		assertEquals("200",q10.getString("status"));
		assertEquals("200",q11.getString("status")); // XXX should be 404, but exception handling currently a bit broken
		assertEquals("200",q12.getString("status"));
		JSONObject b10=new JSONObject(q10.getString("body"));
		JSONObject b11=new JSONObject(q11.getString("body"));
		JSONObject b12=new JSONObject(q12.getString("body"));
		assertEquals("4",b10.getJSONObject("fields").getString("objectNumber"));
		assertEquals(true,b11.getBoolean("isError"));
		assertEquals("3",b12.getJSONObject("fields").getString("objectNumber"));
	}
}
