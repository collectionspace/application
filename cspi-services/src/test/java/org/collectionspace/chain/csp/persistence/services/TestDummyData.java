/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.dom4j.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDummyData extends ServicesBaseClass  {
	private static final Logger log=LoggerFactory.getLogger(TestDummyData.class);
	@Before public void checkServicesRunning() throws ConnectionException {
		setup();
	}

	@Test public void testDataCreation() {
		Storage ss;
		try {
			ss = makeServicesStorage();

			//create objects/procedures/
			
			String objectUrl = create("collectionobjects/", "collectionobjects_common", "dummydata-object1.xml","collection-object");

            String acquisitionUrl = create("acquisitions/", "acquisitions_common", "dummydata-acquisition.xml","acquisition");
			String intakeUrl = create("intakes/", "intakes_common", "dummydata-intake.xml","intake");
			String loaninUrl = create("loansin/", "loansin_common", "dummydata-loanin.xml","loanin");
			String loanoutUrl = create("loansout/", "loansout_common", "dummydata-loanout.xml","loanout");
			String objectexitUrl = create("objectexit/", "objectexit_common", "dummydata-objectexit.xml","objectexit");
	//		String groupUrl = create("group/", "group_common", "dummydata-group.xml","group");
			
			//make relationships

                        relate2way(ss,objectUrl,acquisitionUrl);
			relate2way(ss,objectUrl,intakeUrl);
			relate2way(ss,objectUrl,loaninUrl);
			relate2way(ss,objectUrl,loanoutUrl);
			relate2way(ss,objectUrl,objectexitUrl);

			testRelations(ss,objectUrl,acquisitionUrl,"affects");
			testRelations(ss,objectUrl,intakeUrl,"affects");
			testRelations(ss,objectUrl,loaninUrl,"affects");
			testRelations(ss,objectUrl,loanoutUrl,"affects");
			testRelations(ss,objectUrl,objectexitUrl,"affects");
	//		testRelations(ss,objectUrl,groupUrl,"affects");
		} catch (CSPDependencyException e) {
			fail(e.getMessage());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	private Boolean testRelations(Storage ss, String src, String dst, String type) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{
		

		JSONObject searchRestriction = new JSONObject();
		searchRestriction.put("src",src);
		searchRestriction.put("dst",dst);
		searchRestriction.put("type","affects");
		
///relations?sbj=f3b18013-154d-4e1d-ac5e&obj=c5a79acc-55c6-4046-8f4b
		JSONObject datalist = ss.getPathsJSON("relations/main",searchRestriction);
		String[] listitems = (String[])datalist.get("listItems");
		assertEquals(listitems.length,1);
		return false;
	}
	
	private String relate2way(Storage ss,String obj1,String obj2) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{

		String path3=relate(ss,obj1,obj2);
		String path3a=relate(ss,obj2,obj1);
		assertNotNull("relate one way", path3);
		assertNotNull("relate second way", path3a);
		return path3;
	}
	
	private String relate(Storage ss,String obj1,String obj2) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		
		String[] path1=obj1.split("/");
		String[] path2=obj2.split("/");	
		JSONObject data = createRelation(path2[1],path2[2],"affects",path1[1],path1[2],false);
		// create
		return ss.autocreateJSON("relations/main/",data,null);
	}	
	private String createMini(String type,String id) throws JSONException {
		return type+"/"+id;
	}
	
	private JSONObject createRelation(String src_type,String src,String type,String dst_type,String dst,boolean one_way) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("src",createMini(src_type,src));
		out.put("dst",createMini(dst_type,dst));
		out.put("type",type);
		return out;
	}
	
	private String create(String serviceurl, String partname, String Createfilename, String mungeurl) throws Exception {
		ReturnedURL url;
		
		log.info("Testing " + serviceurl + " with " + Createfilename + " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require uniqueness (to maintain self-contained tests that don't destroy existing data)

		// POST (Create)
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(partname,getDocument(Createfilename));
			url=conn.getMultipartURL(RequestMethod.POST,serviceurl,parts,creds,cache);
		} else {
			url=conn.getURL(RequestMethod.POST,serviceurl,getDocument(Createfilename),creds,cache);
		}

		assertEquals(201,url.getStatus());

		String[] path1=url.getURL().split("/");
		return "/"+mungeurl+"/"+path1[2];
	}
}
