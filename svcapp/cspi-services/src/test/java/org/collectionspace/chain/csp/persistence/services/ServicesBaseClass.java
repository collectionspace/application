package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.junit.Assume;

public class ServicesBaseClass {
	protected ServicesConnection conn;
	protected String base;

	protected void setup() throws BootstrapConfigLoadFailedException, ConnectionException {
		BootstrapConfigController config_controller=new BootstrapConfigController();
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		base=config_controller.getOption("services-url");
		conn=new ServicesConnection(base+"/helloworld/cspace-nuxeo/");
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
		Assume.assumeTrue(out.getStatus()==200);
	}
}
