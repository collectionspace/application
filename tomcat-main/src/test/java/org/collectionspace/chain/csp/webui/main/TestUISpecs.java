/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.persistence.TestBase;
import org.collectionspace.chain.util.json.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUISpecs extends TestBase {
	private static final Logger log = LoggerFactory
			.getLogger(TestUISpecs.class);

	private JSONArray xxxsorted(JSONArray in) throws Exception {
		JSONArray out=new JSONArray();
		Object[] v=new Object[in.length()];
		for(int i=0;i<v.length;i++)
			v[i]=in.get(i);
		try{
			Arrays.sort(v);
		}
		catch(Exception ex){
			log.info("well that was weird, let pretend nothing happened");
		}
		for(int i=0;i<v.length;i++)
			out.put(v[i]);
		return out;
	}
	/* XXX at the moment options are returned unsorted from the service layer, so we need to sort them
	 */
	private void xxxfixOptions_a(JSONArray v) throws Exception {
		for(int i=0;i<v.length();i++) {
			Object x=v.get(i);
			if(x instanceof JSONObject)
				xxxfixOptions((JSONObject)x);
			else if(x instanceof JSONArray){
				v.put(i,xxxsorted((JSONArray)x));
				xxxfixOptions_a((JSONArray)x);
			}
		}
	}
	
	private void xxxfixOptions(JSONObject in) throws Exception {
		if(in.has("optionnames"))
			in.put("optionnames",xxxsorted(in.getJSONArray("optionnames")));
		if(in.has("optionlist"))
			in.put("optionlist",xxxsorted(in.getJSONArray("optionlist")));
		Iterator t = in.keys();
		while(t.hasNext()) {
			String k = (String)t.next();
			Object v = in.get(k);
			if(v!= null) {
				if(v instanceof JSONObject)
					xxxfixOptions((JSONObject)v);
				else if(v instanceof JSONArray) {
					in.put(k,xxxsorted((JSONArray)v));
					xxxfixOptions_a((JSONArray)v);
				}
			}
		}
	}
	
	private void uispec(ServletTester jetty, String url, String uijson)
			throws Exception {

		HttpTester response;
		JSONObject generated;
		JSONObject comparison;

		response = GETData(url, jetty);

		log.info("GENERATED" + response.getContent());
		generated = new JSONObject(response.getContent());
		comparison = new JSONObject(getResourceString(uijson));
		xxxfixOptions(generated);
		xxxfixOptions(comparison);
		
		// You can use these, Chris, to write stuff out if the spec has changed to alter the test file -- dan
		//hendecasyllabic:tmp csm22$ cat gschema.out | pbcopy
		
		//IOUtils.write(generated.toString(),new FileOutputStream("/tmp/gschema.out"));
		//IOUtils.write(comparison.toString(),new FileOutputStream("/tmp/bschema.out"));
		
		log.info("BASELINE" + comparison.toString());
		log.info("GENERATED" + generated.toString());
		assertTrue("Failed to create correct uispec for " + url, JSONUtils
				.checkJSONEquivOrEmptyStringKey(generated, comparison));

	}

	@Test
	public void testUISchema() throws Exception {
		ServletTester jetty = setupJetty();
		//ServletTester jetty=setupJetty(false,"tenant1.xml");
//		uispec(jetty, "/cataloging/serviceschema/collectionspace_core", "collection-object.uischema");
		//uispec(jetty, "/location/uischema", "location.uischema");
		//uispec(jetty, "/reporting/uischema", "recordlist.uischema");
		uispec(jetty, "/recordlist/uischema", "recordlist.uischema");
		uispec(jetty, "/namespaces/uischema", "namespaces.uischema");
		uispec(jetty, "/termlist/uischema", "termlist.uischema");

		uispec(jetty, "/cataloging/uischema", "collection-object.uischema");
		uispec(jetty, "/media/uischema", "media.uischema");
		uispec(jetty, "/acquisition/uischema", "acquisition.uischema");
		uispec(jetty, "/recordtypes/uischema", "recordtypes.uischema");
		
		//serviceschema
//		uispec(jetty, "/cataloging/serviceschema/collectionspace_core", "collection-object.uischema");
	}

	@Test
	public void testUISpec() throws Exception {
		ServletTester jetty = setupJetty();
		
		// XXX this is a mess, the tests are getting too delapidated! -- dan

		//		 uispec(jetty,"/location/generator?quantity=10","acquisition.uispec");
		// uispec(jetty,"/generator?quantity=10&maxrelationships=10&startvalue=0&extraprefix=Related","acquisition.uispec");
		// uispec(jetty,"/person/generator?quantity=10","acquisition.uispec");

//	uispec(jetty, "/cataloging/uispec", "hierarchy.uispec");
//		uispec(jetty, "/cataloging/uischema", "collection-object.uischema");

//		uispec(jetty, "/termlist/uispec", "location.uispec");
		uispec(jetty, "/role/uispec", "roles.uispec");
		
		uispec(jetty, "/users/uispec", "users.uispec");
		uispec(jetty, "/role/uispec", "roles.uispec");
		uispec(jetty, "/permission/uispec", "permissions.uispec");
		uispec(jetty, "/permrole/uispec", "permroles.uispec");


		
		uispec(jetty, "/cataloging/uispec", "collection-object.uispec");
		uispec(jetty, "/person/uispec", "person.uispec");
		uispec(jetty, "/location/uispec", "location.uispec");
//		uispec(jetty, "/organization/uispec", "organization-authority.uispec");
		
		
		uispec(jetty, "/acquisition/uispec", "acquisition.uispec");
		uispec(jetty, "/intake/uispec", "intake.uispec");
		uispec(jetty, "/loanin/uispec", "loanin.uispec");
		uispec(jetty, "/loanout/uispec", "loanout.uispec");
		uispec(jetty, "/movement/uispec", "movement.uispec");
		uispec(jetty, "/objectexit/uispec", "objectexit.uispec");
		uispec(jetty, "/media/uispec", "media.uispec");

		uispec(jetty, "/termlist/uispec", "termlist.uispec");
		uispec(jetty, "/reporting/uispec", "reporting.uispec");
		uispec(jetty, "/invokereport/uispec", "invokereporting.uispec");

//		uispec(jetty, "/organization/uispec", "organization-authority.uispec");
		

//		uispec(jetty, "/cataloging-tab/uispec", "cataloging-tab.uispec");
//		uispec(jetty, "/movement-tab/uispec", "movement-tab.uispec");
			

		uispec(jetty, "/myCollectionSpace/uispec", "find-edit.uispec");

	}
}
