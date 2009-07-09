package org.collectionspace.chain.storage.services;

import org.collectionspace.chain.config.ConfigLoadController;
import org.collectionspace.chain.config.ConfigLoadFailedException;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.junit.Assume;

public class ServicesBaseClass {
	protected ServicesConnection conn;
	protected String base;
	
	protected void setup() throws ConfigLoadFailedException {
		try {
			ConfigLoadController config_controller=new ConfigLoadController(null);
			config_controller.addSearchSuffix("test-config-loader.xml");
			config_controller.go();
			base=config_controller.getOption("services-url");
			conn=new ServicesConnection(base+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
		} catch(BadRequestException e) {
			Assume.assumeTrue(false);
		}
	}
}
