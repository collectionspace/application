/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static org.junit.Assert.*;

import java.util.List;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceThroughAPI extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestServiceThroughAPI.class);

	
	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
	

	//XXX add more tests for other record types
	@Test public void testGetPostDelete() throws Exception {

            	getPostDelete("collection-object/","objectsJSON.json","objectsJSON.json","distinguishingFeatures");

                getPostDelete("acquisition/","acquisitionJSON.json","acquisitionJSON.json","acquisitionReason");
		getPostDelete("loanin/","LoaninJSON.json","LoaninJSON.json","loanInNumber");
		getPostDelete("movement/","movement.json","movement.json","movementReferenceNumber");
		getPostDelete("objectexit/","objectexit.json","objectexit.json","exitNote");

	//	getPostDelete("role/","role.json","role.json","roleName");
	//	getPostDelete("permrole","rolepermissions.xml","rolepermissions.json");
	//	getPostDelete("userrole","accountrole.xml","accountrole.json");
		
		
		//getPostDelete("permission/","permissionsJSON.json","permissionsJSON.json","resourceName");//technically can't update permissions
		
	}
	
	private void getPostDelete(String objtype,String jsoncreate,String jsonupdate,String testfield) throws Exception {
		getPostDelete(objtype,getJSON(jsoncreate),getJSON(jsonupdate),testfield);		
	}
	
	private void getPostDelete(String objtype,JSONObject jsoncreate,JSONObject jsonupdate,String testfield) throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		//create
		String path=ss.autocreateJSON(objtype,jsoncreate);
		//GET and test
		JSONObject jsc=ss.retrieveJSON(objtype+path, new JSONObject());
		assertEquals(jsc.get(testfield),jsoncreate.get(testfield));
		//UPDATE & Test
		ss.updateJSON(objtype+path,jsonupdate);
		JSONObject js=ss.retrieveJSON(objtype+path, new JSONObject());
		assertEquals(js.get(testfield),jsonupdate.get(testfield));
		//DELETE & Test
		ss.deleteJSON(objtype+path);
		try {
			ss.retrieveJSON(objtype+path, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		
	}
	
	
	// XXX factor out
	private static void assertArrayContainsString(String[] a,String b) {
		for(String x: a) {
			if(x.equals(b))
				return;
		}
		assertFalse(true);
	}

	// XXX factor out
	private static void assertArrayDoesNotContainString(String[] a,String b) {
		for(String x: a) {
			if(x.equals(b))
				assertFalse(true);
		}
	}

    public static String getCurrentYear() {
		Calendar cal = GregorianCalendar.getInstance();
        int year = cal.get(Calendar.YEAR);
		return Integer.toString(year);
	}
	
	@Test public void testGetId() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject jo=ss.retrieveJSON("id/intake", new JSONObject());
		assertTrue(jo.getString("next").startsWith("IN" + getCurrentYear() + "."));
		jo=ss.retrieveJSON("id/accession", new JSONObject());
		assertTrue(jo.getString("next").startsWith(getCurrentYear() + ".1."));
	}
	
	public static <T> T[] concat(T[] first, T[] second) {
		  T[] result = Arrays.copyOf(first, first.length + second.length);
		  System.arraycopy(second, 0, result, first.length, second.length);
		  return result;
	}
	
	// XXX use autocreate not create when create dies
	@Test public void testObjectsList() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		String p3=ss.autocreateJSON("collection-object/",getJSON("obj4.json"));
		int num = 0;
		Boolean keeptrying = true;
		String[] names = null;
		String tester = "";
		JSONObject rest = new JSONObject();
		while (keeptrying) {
			rest.put("pageNum", num);
			JSONObject data = ss.getPathsJSON("collection-object", rest);
			String[] names_temp = (String[]) data.get("listItems");
			if (names_temp.length > 0) {
				if (tester.equals(names_temp[0])) {
					keeptrying = false;
				} else {
					tester = names_temp[0];
					if (names == null) {
						names = names_temp;
					} else {
						names = concat(names, names_temp);
					}
				}
			} else {
				keeptrying = false;
			}
			num++;
		}

		assertArrayContainsString(names, p1);
		assertArrayContainsString(names, p2);
		assertArrayContainsString(names, p3);

		ss.deleteJSON("collection-object/"+p1);
		try {
			ss.retrieveJSON("collection-object/"+p1, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		ss.deleteJSON("collection-object/"+p2);
		try {
			ss.retrieveJSON("collection-object/"+p2, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		ss.deleteJSON("collection-object/"+p3);
		try {
			ss.retrieveJSON("collection-object/"+p3, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		
	}
	
	@Test public void testSearch() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("collection-object/",getJSON("obj3.json"));
		String p2=ss.autocreateJSON("collection-object/",getJSON("obj-search.json"));
		JSONObject restriction=new JSONObject();
		restriction.put("keywords","aardvark");
		JSONObject data = ss.getPathsJSON("collection-object",restriction);
		String[] names= (String[])data.get("listItems");
		//XXX add pagination support CSPACE-1836
		assertArrayContainsString(names,p2);
		assertArrayDoesNotContainString(names,p1);
		ss.deleteJSON("collection-object/"+p1);
		ss.deleteJSON("collection-object/"+p2);
		try {
			ss.retrieveJSON("collection-object/"+p2, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
	}
	
	@Test public void testMini() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String p1=ss.autocreateJSON("intake/",getJSON("int4.json"));
		JSONObject mini=ss.retrieveJSON("intake/"+p1+"/view", new JSONObject());
		assertEquals("currentOwnerX",mini.getString("summary"));
		assertEquals("entryNumberX",mini.getString("number"));	
		ss.deleteJSON("intake/"+p1);
		try {
			ss.retrieveJSON("intake/"+p1, new JSONObject());	
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
	}

	private JSONObject makePerson(String pname) throws Exception {
		JSONObject out=new JSONObject();
		out.put("displayName","Dic Penderyn");
		if(pname!=null)
			out.put("group",pname);
		return out;
	}
	@Test public void testAuthorityRefs() throws Exception {
		// Create a record with references
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		JSONObject person=makePerson(null);
		String p=ss.autocreateJSON("person/person",person);
		JSONObject po=ss.retrieveJSON("person/person/"+p, new JSONObject());
		String pname=po.getString("refid");
		//
		JSONObject person2=makePerson(pname);
		String p2=ss.autocreateJSON("person/person",person2);
		//
		JSONObject data=getJSON("int4.json");
		data.remove("valuer");
		data.put("valuer",pname);
		String p1=ss.autocreateJSON("intake/",data);
		JSONObject mini=ss.retrieveJSON("intake/"+p1+"/refs", new JSONObject());
		log.info(mini.toString());
		JSONArray member=mini.getJSONArray("intakes_common:valuer");
		assertNotNull(member);
		Boolean test = false;
		for(int i = 0; i<member.length();i++){
			JSONObject memberitem = member.getJSONObject(i);
			if("Dic Penderyn".equals(memberitem.getString("displayName"))){
				test=true;
			}
		}
		assertTrue(test);
		ss.deleteJSON("person/person/"+p);
		try {
			ss.retrieveJSON("person/person/"+p, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}

		ss.deleteJSON("intake/"+p1);
		try {
			ss.retrieveJSON("intake/"+p1, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		ss.deleteJSON("person/person/"+p2);
		try {
			ss.retrieveJSON("person/person/"+p2, new JSONObject());
			assertFalse(true); // XXX use JUnit exception annotation
		} catch(Exception e) {
			assertTrue(true); // XXX use JUnit exception annotation
		}
		// XXX retrieve by authority
		// XXX also authorities
	}
	

}
