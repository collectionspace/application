package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.kludge.*;
import org.collectionspace.kludge.bootstrap.*;
import org.junit.Assume;

public class ServicesBaseClass {
	protected ServicesConnection conn;
	protected String base;
	
	protected void setup() throws ConfigLoadFailedException, ConnectionException {
			BootstrapConfigController config_controller=new BootstrapConfigController(null);
			config_controller.addSearchSuffix("test-config-loader.xml");
			config_controller.go();
			base=config_controller.getOption("services-url");
			conn=new ServicesConnection(base+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
	}
}
