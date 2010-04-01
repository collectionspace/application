package org.collectionspace.chain.csp.persistence.services;

import java.io.InputStream;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.helper.core.RequestCache;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(ServicesBaseClass.class);
	protected ServicesConnection conn;
	protected String base;
	protected CSPRequestCredentials creds;
	protected CSPRequestCache cache=new RequestCache();

	protected void setup() throws BootstrapConfigLoadFailedException, ConnectionException {
		BootstrapConfigController config_controller=new BootstrapConfigController();
		config_controller.addSearchSuffix("test-config-loader2.xml");
		config_controller.addSearchSuffix("test-config-loader.xml");
		config_controller.go();
		base="http://test.collectionspace.org:8180"; // XXX hardwired
		log.info("base="+base);
		conn=new ServicesConnection(base+"/cspace-services");
		creds=new ServicesRequestCredentials();
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"test");		
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
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
