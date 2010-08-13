package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		log.debug("Services Running!");
	}
	
	protected JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		
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
		//argh - test break when config changes *sob*
		cspm.configure(new InputSource(getRootSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);

		testXMLJSON(spec, "loanin","loaninXMLJSON.xml","LoaninJSON.json");
		testXMLJSON(spec,"acquisition","acquisitionXMLJSON.xml","acquisitionJSON.json");
		testXMLJSON(spec,"collection-object","objectsXMLJSON.xml","objectsJSON.json");
		testXMLJSON(spec,"movement","movement.xml","movement.json");
		testXMLJSON(spec,"role","role.xml","role.json");
		testXMLJSON(spec,"permrole","rolepermissions.xml","rolepermissions.json");
		testXMLJSON(spec, "userrole","accountrole.xml","accountrole.json");
		
		//testXMLJSON(spec, "permission","permissionXMLJSON.xml","permissionsJSON.json");
		//testXMLJSON(spec, "organization","orgauthref.xml","permissionsJSON.json");
	}
	
	@Test public void testJSONXMLConversion() throws Exception {

		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		//argh - test break when config changes *sob*
		cspm.configure(new InputSource(getRootSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);


		testJSONXML(spec, "loanin","loaninXMLJSON.xml","LoaninJSON.json");
		testJSONXML(spec,"acquisition","acquisitionXMLJSON.xml","acquisitionJSON.json");
	//	testJSONXML(spec,"collection-object","objectsXMLJSON.xml","objectsJSON.json");
		testJSONXML(spec,"movement","movement.xml","movement.json");
		testJSONXML(spec,"role","role.xml","role.json");
		
		testJSONXML(spec,"permrole","rolepermissions.xml","rolepermissions.json");
		testJSONXML(spec, "userrole","accountrole.xml","accountrole.json");	
	}

	private void testJSONXML(Spec spec, String objtype, String xmlfile, String jsonfile) throws Exception{

		Record r = spec.getRecord(objtype);
		JSONObject j = getJSON(jsonfile);
		Map<String,Document> parts=new HashMap<String,Document>();
		Document doc = null;
		for(String section : r.getServicesRecordPaths()) {
			String path=r.getServicesRecordPath(section);
			String[] record_path=path.split(":",2);
			doc=XmlJsonConversion.convertToXml(r,j,section);
			parts.put(record_path[0],doc);
		}
//convert json -> xml and back to json and see if it still looks the same..
		JSONObject repeatjson = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion.convertToJson(r, doc);
		log.info(doc.asXML());
		log.info(j.toString());
		log.info(repeatjson.toString());
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(repeatjson,j));
	
	}

	private void testXMLJSON(Spec spec, String objtype, String xmlfile, String jsonfile) throws Exception{

		Document testxml = getDocument(xmlfile);
		String test = testxml.asXML();
		Record r = spec.getRecord(objtype);
		JSONObject repeatjson = org.collectionspace.chain.csp.persistence.services.XmlJsonConversion.convertToJson(r, testxml);
		JSONObject j = getJSON(jsonfile);
		log.info(repeatjson.toString());
		assertTrue(JSONUtils.checkJSONEquivOrEmptyStringKey(repeatjson,j));
	
	}
	@Test public void testPersonContact() throws Exception {
		String serviceurl = "personauthorities/urn:cspace:name(person)/items";
		String filename = "personItem.xml";
		String partname = "persons_common";
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename + " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require uniqueness (to maintain self-contained tests that don't destroy existing data)

		// POST (Create)
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(partname,getDocument(filename));
			url=conn.getMultipartURL(RequestMethod.POST,serviceurl,parts,creds,cache);
		} else {
			url=conn.getURL(RequestMethod.POST,serviceurl,getDocument(filename),creds,cache);
		}

		assertEquals(201,url.getStatus());

		assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g. CSPACE-305 hasn't regressed
		log.info("CREATE PERSON" + url.getURL());
		//create contact person

		String serviceurlContact = "personauthorities/urn:cspace:name(person)/items/"+url.getURLTail()+"/contacts";
		String filenameContact = "personItemContact.xml";
		String partnameContact = "contacts_common";
		log.info("ADD CONTACT USING THIS URL "+ serviceurlContact);
		
		testPostGetDelete(serviceurlContact, partnameContact, "personItemContact.xml", "contacts_common/email", "email@example.com");

		// DELETE (Delete)
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status);		
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(404,status);
		
		log.info("DELETE PERSON");
		// GET once more to make sure it isn't there
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
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
	}

	@Test public void testOrgContact() throws Exception {
		String serviceurl = "orgauthorities/urn:cspace:name(organization)/items";
		String filename = "orgItem.xml";
		String partname = "organizations_common";
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename + " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require uniqueness (to maintain self-contained tests that don't destroy existing data)

		// POST (Create)
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(partname,getDocument(filename));
			url=conn.getMultipartURL(RequestMethod.POST,serviceurl,parts,creds,cache);
		} else {
			url=conn.getURL(RequestMethod.POST,serviceurl,getDocument(filename),creds,cache);
		}

		assertEquals(201,url.getStatus());

		assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g. CSPACE-305 hasn't regressed
		log.info("CREATE ORG" + url.getURL());
		//create contact person

		String serviceurlContact = "orgauthorities/urn:cspace:name(organization)/items/"+url.getURLTail()+"/contacts";
		String filenameContact = "personItemContact.xml";
		String partnameContact = "contacts_common";
		log.info("ADD CONTACT USING THIS URL "+ serviceurlContact);
		
		testPostGetDelete(serviceurlContact, partnameContact, "personItemContact.xml", "contacts_common/email", "email@example.com");

		// DELETE (Delete)
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status);		
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(404,status);
		
		log.info("DELETE ORG");
		// GET once more to make sure it isn't there
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
		assertEquals(404, getStatus); // ensures CSPACE-209 hasn't regressed
		assertNull(doc);
	}
	
	
	@Test public void testAllPostGetDelete() throws Exception {
		// TODO Add vocab (the previous testVocabPost method was just an exact copy of testRolesPost!)
		// TODO Add everything from CSPACE-1876 and more
		//testPostGetDelete("collectionobjects/", "collectionobjects_common", "objectCreate.xml", "collectionobjects_common/objectNumber", "2");
		
		testCRUD("collectionobjects/", "collectionobjects_common", "objectCreate.xml", "objectUpdate.xml", "collectionobjects_common/objectNumber", "2");

		testPostGetDelete("acquisitions/", "acquisitions_common", "acquisitionXMLJSON.xml", "acquisitions_common/accessionDate", "April 1, 2010");
		testPostGetDelete("intakes/", "intakes_common", "intake.xml", "intakes_common/entryNumber","IN2010.2");
		testPostGetDelete("loansin/", "loansin_common", "loaninXMLJSON.xml", "loansin_common/loanInNumber", "LI2010.1.21");
		testPostGetDelete("loansout/", "loansout_common", "loanout.xml", "loansout_common/loanOutNumber", "LO2010.117");

		
		testPostGetDelete("movements/", "movements_common", "movement.xml", "movements_common/movementReferenceNumber", "MV2010.99");
//		testPostGetDelete("relations/", "relations_common", "relationship.xml", "relations_common/relationshipType", "affects");

//		testPostGetDelete("accounts/", null, "account.xml", "accounts_common/userId", "barney");

		// TODO make roleName dynamically vary otherwise POST fails if already exists (something like buildObject)
//		testPostGetDelete("authorization/roles/", null, "role.xml", "role/description", "this role is for test users");
//		testPostGetDelete("authorization/permissions/", null, "permissions.xml", "permission/resourceName", "testthing");

		// TODO might be worth adding test for CSPACE-1947 (POST with wrong label "succeeds")
	}
	
	// TODO merge this method with testPostGetDelete - this is Chris's temporary fork to help testing repeatable fields
	/**
	 * Test Create, Update, Read and Delete for a record type
	 * 
	 * @param serviceurl
	 * @param partname
	 * @param Createfilename
	 * @param Updatefilename
	 * @param xpath
	 * @param expected
	 */
	private void testCRUD(String serviceurl, String partname, String Createfilename, String Updatefilename, String xpath, String expected) throws Exception {
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
		assertEquals(200,getStatus);
		assertNotNull(doc);
		Node n=doc.selectSingleNode(xpath);
		assertNotNull(n);
		String text=n.getText();
		assertEquals(expected,text);	
		
		//Update
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(partname,getDocument(Updatefilename));
			conn.getMultipartXMLDocument(RequestMethod.PUT,url.getURL(),parts,creds,cache);
			ReturnedMultipartDocument rdocs=conn.getMultipartXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdocs.getStatus();
			doc = rdocs.getDocument(partname);
		} else {
			conn.getXMLDocument(RequestMethod.PUT,url.getURL(),getDocument(Updatefilename),creds,cache);
			ReturnedDocument rdoc=conn.getXMLDocument(RequestMethod.GET,url.getURL(),null,creds,cache);
			getStatus = rdoc.getStatus();
			doc = rdoc.getDocument();
		}

		assertEquals(200,getStatus);
		assertNotNull(doc);
		n=doc.selectSingleNode(xpath);
		assertNotNull(n);
		text=n.getText();
		assertEquals(expected,text);
		
		//Get
		
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
	}
	
	private void testPostGetDelete(String serviceurl, String partname, String filename, String xpath, String expected) throws Exception {
		ReturnedURL url;
		log.info("Testing " + serviceurl + " with " + filename + " and partname=" + partname);

		// TODO add document parsing for PUT, and for POSTs that require uniqueness (to maintain self-contained tests that don't destroy existing data)

		// POST (Create)
		if(partname != null) {
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(partname,getDocument(filename));
			url=conn.getMultipartURL(RequestMethod.POST,serviceurl,parts,creds,cache);
		} else {
			url=conn.getURL(RequestMethod.POST,serviceurl,getDocument(filename),creds,cache);
		}

		assertEquals(201,url.getStatus());

		assertTrue(url.getURL().startsWith("/"+serviceurl)); // ensures e.g. CSPACE-305 hasn't regressed

		log.info("CREATE CONTACT "+url.getURL());
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
		assertEquals(200,getStatus);
		log.info("GET CONTACT "+doc.asXML());
		assertNotNull(doc);
		Node n=doc.selectSingleNode(xpath);
		assertNotNull(n);
		String text=n.getText();
		assertEquals(expected,text);	
		
		//List
		log.info("LIST from "+serviceurl);
			ReturnedDocument rdoc1=conn.getXMLDocument(RequestMethod.GET,"/"+serviceurl,null,creds,cache);
			getStatus = rdoc1.getStatus();
			doc = rdoc1.getDocument();
			
		assertEquals(200,getStatus);
		log.info("LISTLISTLIST");
		log.info(doc.asXML());
		log.info("LISTLISTLIST");
		
		
		// DELETE (Delete)
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(200,status);		
		// Now try to delete non-existent (make sure CSPACE-73 hasn't regressed)
		status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		assertEquals(404,status);

		log.info("DELETE");
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
		
	}
	



	//@Test 
	public void testPermissionsPost() throws Exception {
	// TODO check whether this is needed anymore - it should be covered by PostGetDelete above?
	// TODO check whether should be commented back in - Chris was debugging permissions
	//	Map<String,Document> parts=new HashMap<String,Document>();
	//	ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
	//	assertEquals(201,url.getStatus());
	//	int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
	//	assertEquals(200,status); // XXX CSPACE-73, should be 404
	}
	

	//@Test 
	public void testRolePermissionsPost() throws Exception {
	// TODO check whether should be commented back in - Chris was debugging permissions
	// NOTE this test is more complex than PostGetDelete and perhaps should remain as a separate test?
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
	
	@Test public void testAuthorityCreateUpdateDelete() throws Exception {

		Map<String,Document> parts=new HashMap<String,Document>();
		parts.put("personauthorities_common",getDocument("personAuth.xml"));
		String id;
		//CREATE
		ReturnedURL url=conn.getMultipartURL(RequestMethod.POST,"personauthorities/",parts,creds,cache);
		assertEquals(201, url.getStatus());
		id=url.getURLTail();
		//UPDATE
		ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.PUT,"personauthorities/"+id,parts,creds,cache);
		assertEquals(200,doc.getStatus()); // XXX shouldn't this be 201?
		//DELETE
		conn.getNone(RequestMethod.DELETE,"personauthorities/"+id,null,creds,cache);
		assertEquals(200, doc.getStatus());
		
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
