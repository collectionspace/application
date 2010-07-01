package org.collectionspace.chain.storage;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.controller.ChainServlet;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceThroughWebapp {
	private String cookie;
	private static final Logger log=LoggerFactory.getLogger(TestServiceThroughWebapp.class);
	
	// XXX refactor
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	// XXX refactor
	private String getResourceString(String name) throws IOException {
		InputStream in=getResource(name);
		return IOUtils.toString(in,"UTF-8");
	}
	
	// XXX refactor
	private UTF8SafeHttpTester jettyDo(ServletTester tester,String method,String path,String data_str) throws IOException, Exception {
		UTF8SafeHttpTester out=new UTF8SafeHttpTester();
		out.request(tester,method,path,data_str,cookie);
		return out;
	}
	
	private void login(ServletTester tester) throws IOException, Exception {
		UTF8SafeHttpTester out=jettyDo(tester,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
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
		// Login
		login(tester);
		return tester;
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
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));	
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject one = new JSONObject(getResourceString("obj3.json"));
		log.info(one.toString());
		log.info(content.toString());
		assertEquals(one.get("titleLanguage"),content.get("titleLanguage"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("obj3.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+id,makeSimpleRequest(getResourceString("obj4.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+id,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject oneb = new JSONObject(getResourceString("obj4.json"));
		assertEquals(oneb.get("titleLanguage"),content.get("titleLanguage"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("obj4.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+id,null);
		out=jettyDo(jetty,"GET","/chain"+id,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404
	}

	@Test public void testIntake() throws Exception {
		ServletTester jetty=setupJetty();
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(getResourceString("int3.json")));	
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=jettyDo(jetty,"GET","/chain"+path,null);
		log.info(out.getContent());
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject one = new JSONObject(getResourceString("int3.json"));
		assertEquals(one.get("packingNote"),content.get("packingNote"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int3.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+path,makeSimpleRequest(getResourceString("int4.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+path,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject oneb = new JSONObject(getResourceString("int4.json"));
		assertEquals(oneb.get("packingNote"),content.get("packingNote"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int4.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+path,null);
		out=jettyDo(jetty,"GET","/chain"+path,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404		
	}

	@Test public void testAcquisition() throws Exception {
		ServletTester jetty=setupJetty();
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/acquisition/",makeSimpleRequest(getResourceString("create_acquistion.json")));	
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=jettyDo(jetty,"GET","/chain"+path,null);
		JSONObject content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject one = new JSONObject(getResourceString("create_acquistion.json"));
		assertEquals(one.get("acquisitionFundingCurrency"),content.get("acquisitionFundingCurrency"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int5.json")),content));
		out=jettyDo(jetty,"PUT","/chain"+path,makeSimpleRequest(getResourceString("update_acquistion.json")));
		assertEquals(200,out.getStatus());
		out=jettyDo(jetty,"GET","/chain"+path,null);
		content=new JSONObject(out.getContent());
		content=getFields(content);
		JSONObject oneb = new JSONObject(getResourceString("update_acquistion.json"));
		assertEquals(oneb.get("acquisitionFundingCurrency"),content.get("acquisitionFundingCurrency"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(getResourceString("int6.json")),content));		
		out=jettyDo(jetty,"DELETE","/chain"+path,null);
		out=jettyDo(jetty,"GET","/chain"+path,null);
		assertTrue(out.getStatus()!=200); // XXX should be 404		
	}

	@Test public void testIDGenerate() throws Exception {
		ServletTester jetty=setupJetty();
		UTF8SafeHttpTester out=jettyDo(jetty,"GET","/chain/id/intake",null);
		JSONObject jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("IN2010."));
		//test the accessions generated id
		out=jettyDo(jetty,"GET","/chain/id/objects",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("2010.1."));

		//test the loans-in generated id
		out=jettyDo(jetty,"GET","/chain/id/loanin",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LI2010."));

		//test the loans-out generated id
		out=jettyDo(jetty,"GET","/chain/id/loanout",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LO2010."));

		//test the study generated id
		out=jettyDo(jetty,"GET","/chain/id/study",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("ST2010."));

		//test the evaluation generated id
		out=jettyDo(jetty,"GET","/chain/id/evaluation",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("EV2010."));

		//test the library generated id
		out=jettyDo(jetty,"GET","/chain/id/library",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LIB2010."));

		//test the archives generated id
		out=jettyDo(jetty,"GET","/chain/id/archives",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("AR2010."));
	}

	@Test public void testTermsUsed() throws Exception {
		ServletTester jetty=setupJetty();
		
		JSONObject data=new JSONObject("{'fields':{'displayName':'David Bowie'}}");
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/vocabularies/person",data.toString());
		assertEquals(201,out.getStatus());		
		JSONObject jo=new JSONObject(out.getContent());
		String p_csid=jo.getString("csid");
		out=jettyDo(jetty,"GET","/chain/vocabularies/person/"+p_csid,data.toString());
		String p_refid=new JSONObject(out.getContent()).getJSONObject("fields").getString("refid");
		data=new JSONObject(getResourceString("int4.json"));
		data.remove("valuer");
		data.put("valuer",p_refid);
		out=jettyDo(jetty,"POST","/chain/intake/",makeSimpleRequest(data.toString()));
		assertEquals(201,out.getStatus());
		jo=new JSONObject(out.getContent());
		//log.info(jo.toString());
		JSONArray terms_used=jo.getJSONArray("termsUsed");
		assertEquals(1,terms_used.length());
		JSONObject term_used=terms_used.getJSONObject(0);
		//assertEquals("valuer",term_used.getString("sourceFieldName"));
		assertEquals("person",term_used.getString("recordtype"));		
		assertEquals("David Bowie",term_used.getString("number"));
	}
		
	@Test public void testAutoGet() throws Exception {
		ServletTester jetty=setupJetty();
		UTF8SafeHttpTester out=jettyDo(jetty,"GET","/chain/objects/__auto",null);
		assertEquals(200,out.getStatus());
		// XXX this is correct currently, whilst __auto is stubbed.
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(),new JSONObject(out.getContent())));
	}
	
	@Test public void testList() throws Exception {
		ServletTester jetty=setupJetty();
		// delete all
		UTF8SafeHttpTester out=jettyDo(jetty,"GET","/chain/objects",null);
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
		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+id1,null);		
		out=jettyDo(jetty,"DELETE","/chain"+id2,null);
		// check
		assertEquals(id1.split("/")[2],obj1.getString("csid"));
		assertEquals(id2.split("/")[2],obj2.getString("csid"));
		assertEquals("objects",obj1.getString("recordtype"));
		assertEquals("objects",obj2.getString("recordtype"));
		assertEquals("title",obj1.getString("summary"));
		assertEquals("title",obj2.getString("summary"));
		assertEquals("objectNumber",obj1.getString("number"));
		assertEquals("objectNumber",obj2.getString("number"));
	}
	
	@Test public void testSearch() throws Exception {
		ServletTester jetty=setupJetty();
		// one aardvark, one non-aardvark
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3-search.json")));	
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		String good=id1.split("/")[2];
		out=jettyDo(jetty,"POST","/chain/objects/",makeSimpleRequest(getResourceString("obj3.json")));
		String id2=out.getHeader("Location");
		String bad=id2.split("/")[2];
		assertEquals(201,out.getStatus());
		// search
		out=jettyDo(jetty,"GET","/chain/objects/search?query=aardvark",null);
		assertEquals(200,out.getStatus());
		//log.info(out.getContent());
		// check
		JSONArray results=new JSONObject(out.getContent()).getJSONArray("results");
		boolean found=false;
		for(int i=0;i<results.length();i++) {
			String csid=results.getJSONObject(i).getString("csid");
			if(good.equals(csid))
				found=true;
			if(bad.equals(csid))
				assertTrue(false);
		}
		assertTrue(found);
		/* clean up */
		out=jettyDo(jetty,"DELETE","/chain"+id1,null);		
		out=jettyDo(jetty,"DELETE","/chain"+id2,null);
	}
	
	@Test public void testLogin() throws Exception {
		ServletTester jetty=setupJetty();
		UTF8SafeHttpTester out=jettyDo(jetty,"POST","/chain/login","userid=test@collectionspace.org&password=testtest");	
		assertEquals(303,out.getStatus());
		assertEquals("/cspace-ui/html/createnew.html",out.getHeader("Location"));
		out=jettyDo(jetty,"GET","/chain/login?userid=test@collectionspace.org&password=testtest",null);
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
