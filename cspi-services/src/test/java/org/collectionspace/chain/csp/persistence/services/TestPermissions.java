package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPermissions  extends ServicesBaseClass  {
	private static final Logger log=LoggerFactory.getLogger(TestPermissions.class);
	String permission_start = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:permission xmlns:ns2=\"http://collectionspace.org/services/authorization\">";
	String actionC = "<action><name>CREATE</name></action>";
	String actionR = "<action><name>READ</name></action>";
	String actionU = "<action><name>UPDATE</name></action>";
	String actionD = "<action><name>DELETE</name></action>";
	String actionS = "<action><name>SEARCH</name></action>";
	String permission_xmlend = "</ns2:permission>";
	// <resourceName>/authorization/roles/*/permroles/</resourceName> 
	
	String[] data = {"id",
			"idgenerators",
			"/idgenerators/*/ids",
			
			"collectionobjects",
			"/collectionobjects/*/authorityrefs/",
			"intakes",
			"/intakes/*/authorityrefs/",
			"loansin",
			"/loansin/*/authorityrefs/",
			"loansout",
			"/loansout/*/authorityrefs/",
			"movements",
			"/movements/*/authorityrefs/",
			"acquisitions",
			"/acquisitions/*/authorityrefs/",
			
			"vocabularies",
			"vocabularyitems",
			"/vocabularies/*/items/",
			
			"organizations",
			"persons",
			"locations",
			
			"orgauthorities",
			"/orgauthorities/*/items/*/authorityrefs/",
			"/orgauthorities/*/items/",
			"/orgauthorities/*/items/*/refobjs",
			"/orgauthorities/*/items/*/contacts",
			
			"personauthorities",
			"/personauthorities/*/items/*/authorityrefs/",
			"/personauthorities/*/items/*/refobjs",
			"/personauthorities/*/items/",
			"/personauthorities/*/items/*/contacts",
			
			"locationauthorities",
			"/locationauthorities/*/items/*/authorityrefs/",
			"/locationauthorities/*/items/*/refobjs",
			"/locationauthorities/*/items/",
			"/locationauthorities/*/items/*/contacts",
			
			"relations",
			"relations/subject/*/type/*/object/*",
			
			"contacts",
			"notes",
			
			"authorization/roles",
			"authorization/roles/permroles",
			"/authorization/roles/*/permroles/",
			
			"authorization/permissions",
			"authorization/permissions/permroles",
			"/authorization/permissions/*/permroles/",
			
			"accounts",
			"accounts/accountroles",
			"/accounts/*/accountroles/"
			};
	
	 
	@Before public void checkServicesRunning() throws BootstrapConfigLoadFailedException, ConnectionException {
		setup();
	}

	@Test public void testAssumptionMechanism() {
		log.info("Services Running!");
	}

	@Test public void testPermissionsPost() throws Exception {
		Map<String,Document> parts=new HashMap<String,Document>();
		Document doc = getDocument("permissions.xml");
		ReturnedURL url=conn.getURL(RequestMethod.POST,"authorization/permissions/",getDocument("permissions.xml"),creds,cache);
		assertEquals(201,url.getStatus());
		//int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null,creds,cache);
		//assertEquals(200,status); // XXX CSPACE-73, should be 404
	}
	
	// assign permissions to one role
	@Test public void testmultipleAssignPermissions() throws Exception {
		//get all permissions

		ReturnedDocument rdoc=conn.getXMLDocument(RequestMethod.GET,"authorization/permissions/",null,creds,cache);
		int getStatus = rdoc.getStatus();
		Document doc = rdoc.getDocument();
			log.info(doc.toString());
		
		
		
	}

}
