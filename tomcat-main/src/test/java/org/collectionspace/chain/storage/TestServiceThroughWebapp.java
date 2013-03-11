/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.storage;

import static org.junit.Assert.*;

import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.util.json.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceThroughWebapp {
	private static final Logger log=LoggerFactory.getLogger(TestServiceThroughWebapp.class);
	
	private static TestBase tester = new TestBase();
	static ServletTester jetty;
	static {
		try {
			jetty=tester.setupJetty("core",true);
		}
		catch(Exception ex){
			log.error("Could not setup Jetty for test runs", ex);
		}
	}
	
	@BeforeClass public static void testInitialise() throws Exception {
		HttpTester out = tester.GETData(TestBase.AUTHS_INIT_PATH, jetty);
		log.info(out.getContent());
	}
	
	private String getAdminUsername() {
		Spec spec = tester.getSpec(jetty);
		String username = spec.getAdminData().getAuthUser();
		return username;
	}
	
	private String getAdminPassword() {
		Spec spec = tester.getSpec(jetty);
		String pwd = spec.getAdminData().getAuthPass();
		return pwd;
	}
	
	static private String getAdminTenantId() {
		Spec spec = tester.getSpec(jetty);
		String tenant = spec.getAdminData().getTenant();
		return tenant;
	}
	
	/*
	 * Login as the admin user for the tenant
	 */
	@Before public void adminLogin() throws Exception {
		String username = getAdminUsername();
		String pwd = getAdminPassword();
		String tenant = getAdminTenantId();
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/login","userid="+username+"&password="+pwd+"&tenant="+tenant);	
		assertEquals(303,out.getStatus());
		assertEquals("/collectionspace/ui/core/html/findedit.html",out.getHeader("Location"));		
	}
	
	@AfterClass public static void testStop() throws Exception {
		tester.stopJetty(jetty);
	}
	
	@Test public void testCollectionObjectBasic() throws Exception {
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/cataloging/",tester.makeSimpleRequest(tester.getResourceString("obj3.json")));	
		String id=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+id,null);
		JSONObject content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		JSONObject one = new JSONObject(tester.getResourceString("obj3.json"));
		//log.info(one.toString());
		//log.info(content.toString());
                // Haven't yet identified whether JSONObject can use dot-delimited path notation - Aron
		//assertEquals(one.get("titleGroup.0.titleLanguage"),content.get("titleGroup.0.titleLanguage"));
                assertEquals(one.get("distinguishingFeatures"),content.get("distinguishingFeatures"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("obj3.json")),content));
		out=tester.jettyDoUTF8(jetty,"PUT","/tenant/core"+id,tester.makeSimpleRequest(tester.getResourceString("obj4.json")));
		assertEquals(200,out.getStatus());
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+id,null);
		content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		JSONObject oneb = new JSONObject(tester.getResourceString("obj4.json"));
		// assertEquals(oneb.get("titleGroup.0.titleLanguage"),content.get("titleGroup.0.titleLanguage"));
                assertEquals(oneb.get("distinguishingFeatures"),content.get("distinguishingFeatures"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("obj4.json")),content));		
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+id,null);
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+id,null);
		JSONObject bob = new JSONObject(out.getContent());
		assertTrue(bob.getBoolean("isError"));
	}

	@Test public void testIntake() throws Exception {
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/intake/",tester.makeSimpleRequest(tester.getResourceString("int3.json")));	
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		//log.info(out.getContent());
		JSONObject content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		JSONObject one = new JSONObject(tester.getResourceString("int3.json"));
		//XXX we have a utf8 issue so lets not test this
		//assertEquals(one.get("packingNote"),content.get("packingNote"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("int3.json")),content));
		out=tester.jettyDoUTF8(jetty,"PUT","/tenant/core"+path,tester.makeSimpleRequest(tester.getResourceString("int4.json")));
		assertEquals(200,out.getStatus());
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		JSONObject oneb = new JSONObject(tester.getResourceString("int4.json"));
		//XXX we have a utf8 issue so lets not test this
		//assertEquals(oneb.get("packingNote"),content.get("packingNote"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("int4.json")),content));		
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+path,null);
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		JSONObject bob = new JSONObject(out.getContent());
		assertTrue(bob.getBoolean("isError"));
	}

	@Test public void testAcquisition() throws Exception {
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/acquisition/",tester.makeSimpleRequest(tester.getResourceString("create_acquistion.json")));	
		assertEquals(201,out.getStatus());
		String path=out.getHeader("Location");
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		JSONObject content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		log.info(content.toString());
		JSONObject one = new JSONObject(tester.getResourceString("create_acquistion.json"));
		assertEquals(one.get("acquisitionProvisos"),content.get("acquisitionProvisos"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("int5.json")),content));
		out=tester.jettyDoUTF8(jetty,"PUT","/tenant/core"+path,tester.makeSimpleRequest(tester.getResourceString("update_acquistion.json")));
		assertEquals(200,out.getStatus());
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		content=new JSONObject(out.getContent());
		content=tester.getFields(content);
		JSONObject oneb = new JSONObject(tester.getResourceString("update_acquistion.json"));
		assertEquals(oneb.get("acquisitionProvisos"),content.get("acquisitionProvisos"));
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(tester.getResourceString("int6.json")),content));		
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+path,null);
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core"+path,null);
		JSONObject bob = new JSONObject(out.getContent());
		assertTrue(bob.getBoolean("isError"));	
	}

	@Test public void testIDGenerate() throws Exception {
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/intake",null);
		JSONObject jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("IN" + tester.getCurrentYear() + "."));
		//test the accessions generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/accession",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("" + tester.getCurrentYear() + ".1."));

		//test the loans-in generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/loanin",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LI" + tester.getCurrentYear() + "."));

		//test the loans-out generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/loanout",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LO" + tester.getCurrentYear() + "."));

		//test the study generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/study",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("ST" + tester.getCurrentYear() + "."));

		//test the evaluation generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/evaluation",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("EV" + tester.getCurrentYear() + "."));

		//test the library generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/library",null);
		jo=new JSONObject(out.getContent());
		assertTrue(jo.getString("next").startsWith("LIB" + tester.getCurrentYear() + "."));

		//test the archives generated id
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/id/archive",null);
		jo=new JSONObject(out.getContent());
		log.info(out.getContent());
		assertTrue(jo.getString("next").startsWith("AR" + tester.getCurrentYear() + "."));
	}

	@Test public void testTermsUsed() throws Exception {
		JSONObject data=new JSONObject("{'csid':'','fields':{'personTermGroup':[{'termDisplayName':'David Bowie'}]}}");
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/vocabularies/person",data.toString());
		if (out.getStatus() != 201) {
			System.err.println("out.getStatus() != 201");
		}
		assertEquals(201,out.getStatus());		
		JSONObject jo=new JSONObject(out.getContent());
		String p_csid=jo.getString("csid");
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/vocabularies/person/"+p_csid,data.toString());
		String p_refid=new JSONObject(out.getContent()).getJSONObject("fields").getString("refid");
		data=new JSONObject(tester.getResourceString("int4.json"));
		data.remove("valuer");
		data.put("valuer",p_refid);
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/intake/",tester.makeSimpleRequest(data.toString()));
		log.info(out.getContent());
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
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/cataloging/__auto",null);
		assertEquals(200,out.getStatus());
		// XXX this is correct currently, whilst __auto is stubbed.
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(new JSONObject(),new JSONObject(out.getContent())));
	}
	
	@Test public void testList() throws Exception {
		// do not delete all
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/cataloging",null);
		assertEquals(200,out.getStatus());
		JSONObject in=new JSONObject(out.getContent());
		JSONArray items=in.getJSONArray("items");
		// empty
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/cataloging",null);
		assertEquals(200,out.getStatus());
		in=new JSONObject(out.getContent());
		items=in.getJSONArray("items");
		Integer offset = items.length();
		// put a couple in
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/cataloging/",tester.makeSimpleRequest(tester.getResourceString("obj3.json")));	
		String id1=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/cataloging/",tester.makeSimpleRequest(tester.getResourceString("obj3.json")));	
		String id2=out.getHeader("Location");
		assertEquals(201,out.getStatus());
		// size 2, right ones, put them in the right place
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/cataloging",null);
		assertEquals(200,out.getStatus());
		in=new JSONObject(out.getContent());
		items=in.getJSONArray("items");
		/* not a good way to test  fi right ones
		assertEquals(2,items.length()-offset);
		JSONObject obj1=items.getJSONObject(0+offset);
		JSONObject obj2=items.getJSONObject(1+offset);
		if(id2.split("/")[2].equals(obj1.getString("csid"))) {
			JSONObject t=obj1;
			obj1=obj2;
			obj2=t;
		}	
		*/	
		/* clean up */
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+id1,null);		
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+id2,null);
		// check
		/*
		assertEquals(id1.split("/")[2],obj1.getString("csid"));
		assertEquals(id2.split("/")[2],obj2.getString("csid"));
		assertEquals("cataloging",obj1.getString("recordtype"));
		assertEquals("cataloging",obj2.getString("recordtype"));
		assertEquals("title",obj1.getString("summary"));
		assertEquals("title",obj2.getString("summary"));
		assertEquals("objectNumber",obj1.getString("number"));
		assertEquals("objectNumber",obj2.getString("number"));
		*/
	}
	
	@Test public void testSearch() throws Exception {
		// one aardvark, one non-aardvark
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/cataloging/",tester.makeSimpleRequest(tester.getResourceString("obj3-search.json")));	
		assertEquals(201,out.getStatus());
		String id1=out.getHeader("Location");
		String good=id1.split("/")[2];
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/cataloging/",tester.makeSimpleRequest(tester.getResourceString("obj3.json")));
		String id2=out.getHeader("Location");
		String bad=id2.split("/")[2];
		assertEquals(201,out.getStatus());
		// search
		out=tester.jettyDoUTF8(jetty,"GET","/tenant/core/cataloging/search?query=aardvark",null);
		assertEquals(200,out.getStatus());
		log.info(out.getContent());
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
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+id1,null);		
		out=tester.jettyDoUTF8(jetty,"DELETE","/tenant/core"+id2,null);
	}
		
	@Test
	public void testLogin() throws Exception {
		// Should pass because the proper credentials are sent as valid query parameters
		String username = getAdminUsername();
		String pwd = getAdminPassword();
		String tenant = getAdminTenantId();		
		UTF8SafeHttpTester out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/login?userid="+username+"&password="+pwd+"&tenant="+tenant,null);
		assertEquals(303,out.getStatus());
		log.info(out.getHeader("Location"));		
		assertFalse(out.getHeader("Location").endsWith("?result=fail"));
		
		// Should fail because the credentials not valid
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/login", "userid=guest&password=toast&tenant=1");	
		assertEquals(303,out.getStatus());
		assertTrue(out.getHeader("Location").endsWith("?result=fail"));
		
		// Should fail because the credentials are not valid and there is no tenant specified
		out=tester.jettyDoUTF8(jetty,"POST","/tenant/core/login", "userid=bob&password=bob");	
		assertEquals(303,out.getStatus());
		assertTrue(out.getHeader("Location").endsWith("?result=fail"));
		
		// Now that we're finished with the testing, we need to log back in for the other tests to work.
		adminLogin();		
	}
}
