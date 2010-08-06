package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.schema.Record;
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
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	@Test public void testDataCreation() throws Exception{
		String objectUrl = create("collectionobjects/", "collectionobjects_common", "dummydata-object1.xml","collection-object");
		String intakeUrl = create("intakes/", "intakes_common", "dummydata-intake.xml","intake");
		String loaninUrl = create("loansin/", "loansin_common", "dummydata-loanin.xml","loanin");
		String loanoutUrl = create("loansout/", "loansout_common", "dummydata-loanout.xml","loanout");
//		String acquisitionUrl = create("acquisitions/", "acquisitions_common", "dummydata-acquisition.xml","acquisition");
//		log.info(objectUrl);

		Storage ss=makeServicesStorage(base+"/cspace-services/");
		
		//argh uses id not serviceurl

		String path=relate(ss,objectUrl,intakeUrl);
		String path2=relate(ss,intakeUrl,objectUrl);
		

		JSONObject datalist = ss.getPathsJSON("relations/main",null);
		
		log.info(datalist.toString());
		
		log.info("objectUrl"+objectUrl);
		log.info("intakeUrl"+intakeUrl);
		log.info("loaninUrl"+loaninUrl);
		log.info("loanoutUrl"+loanoutUrl);
//		log.info("acquisitionUrl"+acquisitionUrl);
		log.info(path);
		

	}
	
	private String relate(Storage ss,String obj1,String obj2) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		
		String[] path1=obj1.split("/");
		String[] path2=obj2.split("/");	
		JSONObject data = createRelation(path2[1],path2[2],"affects",path1[1],path1[2],false);
		// create
		return ss.autocreateJSON("relations/main/",data);
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
