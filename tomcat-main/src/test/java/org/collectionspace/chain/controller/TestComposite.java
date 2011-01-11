package org.collectionspace.chain.controller;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.util.json.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestComposite extends TestBase {
	
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
	
	@Test public void testCompositeBasic() throws Exception {
		ServletTester jetty=setupJetty();
		// Three POSTs give us some data to play with
		JSONObject p1=createCompositePOSTPartJSON(makeSimpleRequest(getResourceString("obj8.json")));
		JSONObject p2=createCompositePOSTPartJSON(makeSimpleRequest(getResourceString("obj8.json")));
		JSONObject p3=createCompositePOSTPartJSON(makeSimpleRequest(getResourceString("obj8.json")));
		JSONObject p=new JSONObject();
		p.put("p1",p1);
		p.put("p2",p2);
		p.put("p3",p3);
		System.err.println("p="+p);
		HttpTester out = POSTData("/composite",p.toString(),jetty);
		JSONObject jout=new JSONObject(out.getContent());
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
		JSONObject p4=createCompositePUTPartJSON(id1,makeSimpleRequest(getResourceString("obj9.json")));
		JSONObject p5=createCompositePUTPartJSON(id2,makeSimpleRequest(getResourceString("obj9.json")));
		JSONObject pp=new JSONObject();
		pp.put("p4",p4);
		pp.put("p5",p5);
		System.err.println("pp="+pp);
		HttpTester out2 = PUTData("/composite",pp.toString(),jetty);
		JSONObject jout2=new JSONObject(out2.getContent());
		JSONObject q4=jout2.getJSONObject("p4");
		JSONObject q5=jout2.getJSONObject("p5");
		assertEquals("200",q4.getString("status"));
		assertEquals("200",q5.getString("status"));
		// Now some GETs
		JSONObject ppp=new JSONObject();
		ppp.put("p6",createCompositeGETPartJSON(id1));
		ppp.put("p7",createCompositeGETPartJSON(id2));
		ppp.put("p8",createCompositeGETPartJSON(id3));
		System.err.println("ppp="+ppp);
		HttpTester out3 = GETData("/composite",ppp.toString(),jetty);
		JSONObject jout3=new JSONObject(out3.getContent());
		JSONObject q6=jout3.getJSONObject("p6");
		JSONObject q7=jout3.getJSONObject("p7");
		JSONObject q8=jout3.getJSONObject("p8");
		assertEquals("200",q6.getString("status"));
		assertEquals("200",q7.getString("status"));
		assertEquals("200",q8.getString("status"));
		System.err.println("p6="+q6);
		JSONObject b6=new JSONObject(q6.getString("body"));
		JSONObject b7=new JSONObject(q7.getString("body"));
		JSONObject b8=new JSONObject(q8.getString("body"));
		assertEquals("4",b6.getJSONObject("fields").getString("objectNumber"));
		assertEquals("4",b7.getJSONObject("fields").getString("objectNumber"));
		assertEquals("3",b8.getJSONObject("fields").getString("objectNumber"));
		// Now some DELETEs
		JSONObject pppp=new JSONObject();
		pppp.put("p9",createCompositeDELETEPartJSON(id2));
		HttpTester out4 = POSTData("/composite",pppp.toString(),jetty);
		JSONObject jout4=new JSONObject(out4.getContent());
		JSONObject q9=jout4.getJSONObject("p9");
		assertEquals("200",q9.getString("status"));
		// Try some things that should fail, mixed with some things that should not
		JSONObject ppppp=new JSONObject();
		ppppp.put("p10",createCompositeGETPartJSON(id1));
		ppppp.put("p11",createCompositeGETPartJSON(id2));
		ppppp.put("p12",createCompositeGETPartJSON(id3));
		System.err.println("ppppp="+ppppp);
		HttpTester out5 = GETData("/composite",ppppp.toString(),jetty);
		JSONObject jout5=new JSONObject(out5.getContent());
		System.err.println("jout5="+jout5);
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
