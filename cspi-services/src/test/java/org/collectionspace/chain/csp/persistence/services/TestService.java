package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.container.impl.CSPManagerImpl;

public class TestService extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestService.class);
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	@Test public void testAssumptionMechanism() {
		log.info("Services Running!");
	}
	private InputStream getRootSource(String file) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}

	protected JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		//log.info(path);
		assertNotNull(stream);
		String data=IOUtils.toString(stream,"UTF-8");
		stream.close();		
		return new JSONObject(data);
	}
	
	@Test public void testXMLJSONConversion() throws Exception {

		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		cspm.configure(new InputSource(getRootSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);

		Document repeatxml = getDocument("loaninXMLJSON.xml");
		Record r = spec.getRecord("loanin");
		Document repeatxml2 = getDocument("permissionXMLJSON.xml");
		Record r2 = spec.getRecord("permission");
		JSONObject j1 = getJSON("LoaninJSON.json");
		JSONObject j2 = getJSON("permissionsJSON.json");
		//JSONObject repeatjson = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion.convertToJson(r, repeatxml);
		//log.info(repeatjson.toString());
		//assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(repeatjson,j1));
		JSONObject repeatjson2 = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion.convertToJson(r2, repeatxml2);
		//log.info(repeatjson2.toString());
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(repeatjson2,j2));
	}

	@Test public void testAllPostGetDelete() throws Exception {
		// TODO Change the XML filenames to be more meaningful
		// TODO Add vocab (the previous testVocabPost method was just an exact copy of testRolesPost!)
		// TODO Add everything from CSPACE-1876 and more
		
		testPostGetDelete("collectionobjects/", "collectionobjects_common", "obj1.xml", "collectionobjects_common/objectNumber", "2");
		
		// TODO make roleName dynamically vary otherwise POST fails if already exists (something like buildObject)
		testPostGetDelete("authorization/roles/", null, "obj5.xml", "role/description", "this role is for test users");
		testPostGetDelete("authorization/permissions/", null, "permissions.xml", "permission/resourceName", "testthing");
		
		//testPostGetDelete("accounts/", null, "account.xml", "account/userid", "accounts");

		// XXX Queries about movement service consistency currently being discussed by email:
		// - apparently inconsistent naming of label for POST (see xxx_hack_mov)
		// - ought it have more stuff inside? currently returning an empty movementMethods
		// - apparently it is possible to POST using label collectionobjects_common too!?
		//   (although it will still return the right label)
		//testPostGetDelete("movements/", "movements_common", "movement.xml", "movements_common/movementMethods", "");	
	}
	
	private String xxx_hack_for_mov(String partname) {
		return partname.replace("movements_common", "movement_common");
	}

	private void testPostGetDelete(String serviceurl, String partname, String filename, String xpath, String expected) throws Exception {
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename + " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require uniqueness (to maintain self-contained tests that don't destroy existing data)

		// POST (Create)
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(xxx_hack_for_mov(partname),getDocument(filename));
			url=conn.getMultipartURL(RequestMethod.POST,serviceurl,parts,creds,cache);
		} else {
			url=conn.getURL(RequestMethod.POST,serviceurl,getDocument(filename),creds,cache);
		}

		assertEquals(201,url.getStatus());

		log.info("POST returned "+url.getURL());
		assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g. CSPACE-305 hasn't regressed
		
		// GET (Read)
		int getStatus;
		Document doc; 
		if(partname != null) {
			ReturnedMultipartDocument rdocs=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc=conn.getXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		log.info("MYXML",doc.asXML());
		assertEquals(200,getStatus);
		assertNotNull(doc);
		Node n=doc.selectSingleNode(xpath);
		assertNotNull(n);
		String text=n.getText();
		assertEquals(expected,text);	
		
		// DELETE (Delete)
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status);		
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(404,status);
		
		// GET once more to make sure it isn't there
		if(partname != null) {
			ReturnedMultipartDocument rdocs=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			ReturnedDocument rdoc=conn.getXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
		
		log.info("DONE");
	}
	



	//@Test 
	public void testPermissionsPost() throws Exception {
	//	Map<String,Document> parts=new HashMap<String,Document>();
	//	ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
	//	assertEquals(201,url.getStatus());
	//	int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	//	assertEquals(200,status); // XXX CSPACE-73, should be 404
	}
	

	//@Test 
	public void testRolePermissionsPost() throws Exception {
		//create a permission
	//	Map<String,Document> parts=new HashMap<String,Document>();
	//	ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
	//	assertEquals(201,url.getStatus());

		//create permissionRole for the permission above
	//	url = conn.getURL(RequestMethod.POST, "authorization/permissions/"+url.getURLTail()+"/permroles", getDocument("rolepermissions.xml"), creds, cache);
	//	assertEquals(201, url.getStatus());
		//delete the permissionRole
	//	int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	//	assertEquals(200,status); // XXX CSPACE-73, should be 404

		//delete the permission
	//	status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	//	assertEquals(200,status); // XXX CSPACE-73, should be 404
	}

	@Test public void testObjectsPut() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("collectionobjects_common",getDocument("obj1.xml"));		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.PUT,url.getURL(),buildObject("32","obj2.xml","collectionobjects_common"),creds,cache);
		assertEquals(201,url.getStatus()); // 201?
		doc=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument("collectionobjects_common").selectSingleNode("collectionobjects_common/objectNumber").getText();
		assertEquals("32",num);
	}


	// TODO pre-emptive cache population
	
	private Map<String,Document> buildObject(String objid,String src,String part) throws DocumentException, IOException {
		InputStream data_stream=getResource(src);
		String data=IOUtils.toString(data_stream);
		data_stream.close();
		data=data.replaceAll("<<objnum>>",objid);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(new StringReader(data));
		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put(part,doc);
		return parts;
	}
	
	@Test public void testSearch() throws Exception{
		// Insert one non-aardvark
		Map<String,Document> parts=new HashMap<String,Document>();
		Document doc1=getDocument("obj1.xml");
		parts.put("collectionobjects_common",doc1);		
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		assertEquals(201,url.getStatus());
		String uid1 = url.getURL();
		String non=url.getURLTail();
		// Insert one aardvark
		parts=new HashMap<String,Document>();
		Document doc2=getDocument("obj-search.xml");
		parts.put("collectionobjects_common",doc2);		
		url=conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts,creds,cache);
		String uid2 = url.getURL();
		assertEquals(201,url.getStatus());
		String good=url.getURLTail();		
		// search for aardvark
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.GET,"collectionobjects?kw=aardvark",null,creds,cache);
		assertEquals(200,doc.getStatus());
		Set<String> csids=new HashSet<String>();
		for(Node n : (List<Node>)doc.getDocument().selectNodes("collectionobjects-common-list/collection-object-list-item/csid")) {
			csids.add(n.getText());
		}

		//delete non-aadvark and aadvark
		int status=conn.getNone(RequestMethod.DELETE,uid1,null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
		status=conn.getNone(RequestMethod.DELETE,uid2,null,creds,cache);
		assertEquals(200,status); // XXX CSPACE-73, should be 404
		
		//test
		assertFalse(csids.size()==0);
		assertTrue(csids.contains(good));
		assertFalse(csids.contains(non));
		
		
	}
}
