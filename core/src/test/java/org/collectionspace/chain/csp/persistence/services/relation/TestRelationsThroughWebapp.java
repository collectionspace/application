package org.collectionspace.chain.csp.persistence.services.relation;

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
	
	private JSONObject makeMini(String type,String dst) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("relationshiptype",type);
		if(dst.startsWith("/"))
			dst=dst.substring(1);
		String[] dsts=dst.split("/");
		out.put("recordtype",dsts[0]);
		out.put("csid",dsts[1]);
		return out;
	}
	
	private String makeSimpleRequest(String in) throws JSONException {
		return makeRequest(new JSONObject(in),null).toString();
	}
	
	/* [19:35] danatcaret: Oh, I've found the tests now, anastasiac, for future reference, if you want to create 
	 *           some relations for testing in future. They're in [this file, ed], though it's a bit highly 
	 *           factored, so I'd recommend either single stepping what it's doing, or a strong drink, before 
	 *           looking at it, :).
     * [19:35] anastasiac: ah, yes, I was looking at that file already. I agree with the strong drink recommendation 
     * [19:40] danatcaret: :). The method makeMini creates JSON for those standardised "mini" records we were 
     * 			 talking about in the STIM, the ones which go in arrays in lists, search results, etc, for testing. 
     * 			 Lines 90-96 just create two records, with csids id1, id2. r1 and r2 are corresponding "mini" 
     * 			 records to id1 and id2. Then on line 114, r1 and r2 are both sent along with some random (ignored) 
     * 			 field data (from obj3.json) via makeRecord to create a new record. The name of that is put into 
     * 			 id3. Because r1 and r2 were sent when id3 was created, there are relations to id1 and id2 in id3. 
     * 			 At the moment the code just retrieves id3, and prints it out. What it will do in the end is check 
     * 			 that the relations to id1 and id2 are present. (adding to code as comment).
	 */
	@Test public void testRelationsThroughWebapp() throws Exception {
		ServletTester jetty=setupJetty();
		// First create a couple of objects
		HttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		assertEquals(201,out.getStatus());
		String id2=out.getHeader("Location");
		// Now create one with a pair of relations in: 3->1, 3->2
		JSONObject r1=makeMini("affects",id1);
		JSONObject r2=makeMini("affects",id2);
		out=jettyDo(jetty,"POST","/chain/objects/",makeRequest(new JSONObject(getResourceString("obj3.json")),
															   new JSONObject[]{r1,r2}).toString());
		assertEquals(201,out.getStatus());
		String id3=out.getHeader("Location");
		System.err.println("id3="+id3);
		// Now do a retrieve on all three, check they have the right number of relations
		out=jettyDo(jetty,"GET","/chain"+id3,null);
		assertEquals(200,out.getStatus());
		System.err.println(out.getContent());
	}
}
