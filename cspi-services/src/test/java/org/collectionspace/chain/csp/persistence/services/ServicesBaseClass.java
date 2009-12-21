package org.collectionspace.chain.csp.persistence.services;

import java.io.InputStream;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Assume;

public class ServicesBaseClass {
	protected ServicesConnection conn;
	protected String base;

	protected void setup() throws BootstrapConfigLoadFailedException, ConnectionException {
		BootstrapConfigController config_controller=new BootstrapConfigController();
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.go();
		base=config_controller.getOption("services-url");
		System.err.println("base="+base);
		conn=new ServicesConnection(base+"/cspace-services");
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null);
		Assume.assumeTrue(out.getStatus()==200);
	}
	
	// XXX refactor
	protected Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}

	// XXX refactor
	protected InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
}
