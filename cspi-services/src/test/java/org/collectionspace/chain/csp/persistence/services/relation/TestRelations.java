package org.collectionspace.chain.csp.persistence.services.relation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.ServicesBaseClass;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.core.RequestCache;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestRelations extends ServicesBaseClass {

	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	// XXX refactor
	private JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		assertNotNull(stream);
		String data=IOUtils.toString(stream);
		stream.close();		
		return new JSONObject(data);
	}
	
	private Map<String,Document> getMultipartCommon(String part,String file) throws DocumentException {
		return makeMultipartCommon(part,getDocument(file));
	}

	private Map<String,Document> makeMultipartCommon(String part,Document doc) throws DocumentException {
		Map<String,Document> out=new HashMap<String,Document>();
		out.put(part,doc);
		return out;
	}
	
	private static Storage makeServicesStorage(String path) throws CSPDependencyException {
		return new ServicesStorageGenerator(path).getStorage(new RequestCache());
	}
	
	@Test public void testRelations() throws Exception {
		// Quick rocket through direct PUT/POST/GET/DELETE to check everything is in order before we use the API
		RelationFactory factory=new RelationFactory();
		Relation r1=factory.create("SubjectType-1261070872573-type","Subject-1261070872573","collectionobject-intake","ObjectType-1261070872573-type","Object-1261070872573");		
		ReturnedURL doc2=conn.getMultipartURL(RequestMethod.POST,"/relations/",makeMultipartCommon("relations_common",r1.toDocument()));
		assertTrue(doc2.getStatus()<300);
		System.err.println("url="+doc2.getURL());
		ReturnedMultipartDocument doc3=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null);
		assertTrue(doc3.getStatus()<300);		
		System.err.println(doc3.getDocument("relations_common").asXML());
		Relation r2=factory.load(doc3.getDocument("relations_common"));
		assertEquals(r1.getSourceType(),r2.getSourceType());
		assertEquals(r1.getDestinationType(),r2.getDestinationType());
		assertEquals(r1.getSourceId(),r2.getSourceId());
		assertEquals(r1.getDestinationId(),r2.getDestinationId());
		assertEquals(r1.getRelationshipType(),r2.getRelationshipType());
		Relation r3=factory.create("ZSubjectType-1261070872573-type","ZSubject-1261070872573","Zcollectionobject-intake","ZObjectType-1261070872573-type","ZObject-1261070872573");		
		ReturnedMultipartDocument doc4=conn.getMultipartXMLDocument(RequestMethod.PUT,doc2.getURL(),makeMultipartCommon("relations_common",r3.toDocument()));
		assertTrue(doc4.getStatus()<300);
		ReturnedMultipartDocument doc5=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null);
		assertTrue(doc5.getStatus()<300);		
		Relation r4=factory.load(doc5.getDocument("relations_common"));
		assertEquals(r3.getSourceType(),r4.getSourceType());
		assertEquals(r3.getDestinationType(),r4.getDestinationType());
		assertEquals(r3.getSourceId(),r4.getSourceId());
		assertEquals(r3.getDestinationId(),r4.getDestinationId());
		assertEquals(r3.getRelationshipType(),r4.getRelationshipType());
		int status=conn.getNone(RequestMethod.DELETE,doc2.getURL(),null);
		assertTrue(status<300);
		ReturnedMultipartDocument doc6=conn.getMultipartXMLDocument(RequestMethod.GET,doc2.getURL(),null);
		assertTrue(doc6.getStatus()>299);
	}
	
	private String makeRecord(Storage ss,String id) throws Exception {
		JSONObject obj=getJSON("obj3.json");
		obj.put("accessionNumber",id);
		return ss.autocreateJSON("collection-object/",obj);
	}
	
	@Test public void testRelationsThroughAPI() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		String obj1=makeRecord(ss,"A");
		String obj2=makeRecord(ss,"B");
		String obj3=makeRecord(ss,"C");
		JSONObject data=new JSONObject();
		data.put("src",obj1);
		data.put("dst",obj2);
		data.put("type","test");
		// create
		String path=ss.autocreateJSON("relations/main/",data);
		System.err.println("path="+path);
		// get
		JSONObject data2=ss.retrieveJSON("relations/main/"+path);
		JSONUtils.checkJSONEquiv(data,data2);
		// update
		data.put("dst",obj3);
		ss.updateJSON("/relations/main/"+path,data);
		// get
		JSONObject data3=ss.retrieveJSON("relations/main/"+path);
		JSONUtils.checkJSONEquiv(data,data3);		
		// delete
		ss.deleteJSON("/relations/main/"+path);
		// get
		try {
			ss.retrieveJSON("relations/main/"+path);
			assertTrue(false);
		} catch(ExistException x) {}
	}
}
