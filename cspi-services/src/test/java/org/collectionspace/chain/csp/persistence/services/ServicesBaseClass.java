package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.RequestCache;
import org.collectionspace.csp.helper.test.TestConfigFinder;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

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
		base=TestConfigFinder.xxx_servicesBaseURL; // XXX still yuck but centralised now

		conn=new ServicesConnection(base+"/cspace-services");
		creds=new ServicesRequestCredentials();
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"testtest");		
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		Assume.assumeTrue(out.getStatus()==200);
	}
	
	// XXX refactor
	protected JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);

		assertNotNull(stream);
		String data=IOUtils.toString(stream,"UTF-8");
		stream.close();		
		return new JSONObject(data);
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
	
	protected InputStream getRootSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream();
		} catch (CSPDependencyException e) {
			log.debug("Falling back to trying to find old config file");
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(fallbackFile);
		}
	}
	
	protected Storage makeServicesStorage(String path) throws CSPDependencyException {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		cspm.configure(new InputSource(getRootSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		Record r_obj=spec.getRecord("collection-object");
		assertNotNull(r_obj);
		assertEquals("collection-object",r_obj.getID());
		assertEquals("objects",r_obj.getWebURL());
		StorageGenerator gen=cspm.getStorage("service");
		CSPRequestCredentials creds=gen.createCredentials();
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"testtest");
		return gen.getStorage(creds,new RequestCache());
	}
}
