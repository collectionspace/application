package org.collectionspace.chain.csp.persistence.services.relation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.ServicesBaseClass;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRelations extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestRelations.class);
	// XXX refactor
	private String getResourceString(String name) throws IOException {
		InputStream in=getResource(name);
		return IOUtils.toString(in);
	}
	
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}
	
	private Map<String,Document> getMultipartCommon(String part,String file) throws DocumentException {
		return makeMultipartCommon(part,getDocument(file));
	}

	private Map<String,Document> makeMultipartCommon(String part,Document doc) throws DocumentException {
		Map<String,Document> out=new HashMap<String,Document>();
		out.put(part,doc);
		return out;
	}
			
	@Test public void testRelations() throws Exception {
		// Quick rocket through direct PUT/POST/GET/DELETE to check everything is in order before we use the API
		RelationFactory factory=new RelationFactory();
		Relation r1=factory.create(null,"SubjectType-1261070872573-type","Subject-1261070872573","collectionobject-intake","ObjectType-1261070872573-type","Object-1261070872573");		
		ReturnedURL doc2=conn.getMultipartURL(RequestMethod.POST,"/relations/",makeMultipartCommon("relations_common",r1.toDocument()),creds,cache);
		assertTrue(doc2.getStatus()<300);

		ReturnedMultipartDocument doc3=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null,creds,cache);
		assertTrue(doc3.getStatus()<300);		

		Relation r2=factory.load(null,doc3.getDocument("relations_common"));
		assertEquals(r1.getSourceType(),r2.getSourceType());
		assertEquals(r1.getDestinationType(),r2.getDestinationType());
		assertEquals(r1.getSourceId(),r2.getSourceId());
		assertEquals(r1.getDestinationId(),r2.getDestinationId());
		assertEquals(r1.getRelationshipType(),r2.getRelationshipType());
		Relation r3=factory.create(null,"ZSubjectType-1261070872573-type","ZSubject-1261070872573","Zcollectionobject-intake","ZObjectType-1261070872573-type","ZObject-1261070872573");		
		ReturnedMultipartDocument doc4=conn.getMultipartXMLDocument(RequestMethod.PUT,doc2.getURL(),makeMultipartCommon("relations_common",r3.toDocument()),creds,cache);
		assertTrue(doc4.getStatus()<300);
		ReturnedMultipartDocument doc5=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null,creds,cache);
		assertTrue(doc5.getStatus()<300);		
		Relation r4=factory.load(null,doc5.getDocument("relations_common"));
		assertEquals(r3.getSourceType(),r4.getSourceType());
		assertEquals(r3.getDestinationType(),r4.getDestinationType());
		assertEquals(r3.getSourceId(),r4.getSourceId());
		assertEquals(r3.getDestinationId(),r4.getDestinationId());
		assertEquals(r3.getRelationshipType(),r4.getRelationshipType());
		int status=conn.getNone(RequestMethod.DELETE,doc2.getURL(),null,creds,cache);
		assertTrue(status<300);
		ReturnedMultipartDocument doc6=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null,creds,cache);
		assertTrue(doc6.getStatus()>299);
	}
	
	private String makeRecord(Storage ss,String id) throws Exception {
		JSONObject obj=getJSON("obj3.json");
		obj.put("accessionNumber",id);
		return ss.autocreateJSON("collection-object/",obj);
	}
	
	@Test public void testRelationsThroughAPI() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		//create 3 objects
		String obj1=makeRecord(ss,"A");
		String obj2=makeRecord(ss,"B");
		String obj3=makeRecord(ss,"C");
		
		// relate obj1 and obj2
		String path=relate(ss,obj1,obj2);
		//relate  obj2 and obj3
		String path2=relate(ss,obj2,obj3);
		
		// test relationship
		JSONObject data2=ss.retrieveJSON("relations/main/"+path);
		assertTrue(JSONUtils.checkJSONEquiv("collection-object/"+obj1,data2.getString("src")));
		assertTrue(JSONUtils.checkJSONEquiv("collection-object/"+obj2,data2.getString("dst")));
		assertTrue(JSONUtils.checkJSONEquiv("affects",data2.getString("type")));
		
		// update
		updaterelate(ss,path,obj1,obj3);
		
		// get
		JSONObject data3=ss.retrieveJSON("relations/main/"+path);
		assertTrue(JSONUtils.checkJSONEquiv("collection-object/"+obj1,data3.getString("src")));
		assertTrue(JSONUtils.checkJSONEquiv("collection-object/"+obj3,data3.getString("dst")));
		assertTrue(JSONUtils.checkJSONEquiv("affects",data3.getString("type")));
		
		//get list

		JSONObject searchRestriction = new JSONObject();
		searchRestriction.put("src","collection-object/"+obj2);
		searchRestriction.put("type","affects");

		// simple list
		//XXX CSPACE-1080 - will need to update if this is improved
		JSONObject datalist = ss.getPathsJSON("relations/main",searchRestriction);
		int truecount = 0;
		String[] paths=(String[])datalist.get("listItems");
		log.info(datalist.toString());
		JSONObject pagination = datalist.getJSONObject("pagination");
		boolean pagbool = false;
		for(int i=0;i<paths.length;i++){
			if(paths[i].equals(path) || paths[i].equals(path2)){
				truecount++;
			}
		}
		
		assertTrue(truecount>0);
		
		// delete
		ss.deleteJSON("/relations/main/"+path);
		ss.deleteJSON("/relations/main/"+path2);
		
		// delete objects
		ss.deleteJSON("collection-object/"+obj1);
		ss.deleteJSON("collection-object/"+obj2);
		ss.deleteJSON("collection-object/"+obj3);
		
		// get
		try {
			ss.retrieveJSON("relations/main/"+path);
			assertTrue(false);
		} catch(ExistException x) {
			assertTrue(true);
		}
	}

	private String relate(Storage ss,String obj1,String obj2) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject data=new JSONObject();
		data.put("src","collection-object/"+obj1);
		data.put("dst","collection-object/"+obj2);
		data.put("type","affects");
		// create
		return ss.autocreateJSON("relations/main/",data);
	}
	private void updaterelate(Storage ss,String path, String obj1,String obj2) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject data=new JSONObject();
		data.put("src","collection-object/"+obj1);
		data.put("dst","collection-object/"+obj2);
		data.put("type","affects");
		// udpate
		ss.updateJSON("relations/main/"+path, data);
	}
	
}
