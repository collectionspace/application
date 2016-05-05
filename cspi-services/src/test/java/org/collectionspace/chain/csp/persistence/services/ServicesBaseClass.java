/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
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
import org.collectionspace.csp.helper.core.ConfigFinder;
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
	protected String base = null;
	protected CSPRequestCredentials creds;
	protected CSPRequestCache cache=new RequestCache();

	protected void setup() throws ConnectionException {
		Spec spec = null;
		String ims_base="";
		try {
			base=getBaseUrl();
			ims_base=getIMSBaseUrl();
			spec = getDefaultSpec();
		} catch (CSPDependencyException e) {
			assertNotNull("Base service url invalid in config file",base);
		} // XXX still yuck but centralised now
		log.info("ServicesBaseClass setting up connection using base URL:"+base);

		conn=new ServicesConnection(base,ims_base);
		creds=new ServicesRequestCredentials();

		creds.setCredential(ServicesStorageGenerator.CRED_USERID,spec.getAdminData().getAuthUser());
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,spec.getAdminData().getAuthPass());

		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"accounts/0/accountperms",null,creds,cache);
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
	
	protected InputSource getRootSource() throws CSPDependencyException {
		try {
			return TestConfigFinder.getConfigStream();
		} catch (CSPDependencyException e) {
			log.error("Failed to find config file");
			throw new CSPDependencyException("failed to find config file");
		}
	}
	private CSPManager getServiceManager() throws CSPDependencyException{
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go();
		cspm.configure(getRootSource(), new ConfigFinder(null), false);
		return cspm;
		
	}
	private String getBaseUrl() throws CSPDependencyException{
		CSPManager cspm=getServiceManager();
		ServicesStorageGenerator gen=(ServicesStorageGenerator)cspm.getStorage("service");
		String baseurl = gen.getBase();
		return baseurl;
	}

	private String getIMSBaseUrl() throws CSPDependencyException{
		CSPManager cspm=getServiceManager();
		ServicesStorageGenerator gen=(ServicesStorageGenerator)cspm.getStorage("service");
		String baseurl = gen.getIMSBase();
		return baseurl;
	}
	
	private Spec getDefaultSpec() throws CSPDependencyException {
		CSPManager cspm=getServiceManager();
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		return spec;
	}
	
	protected Storage makeServicesStorage() throws CSPDependencyException {
		CSPManager cspm=getServiceManager();
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		
		//XXX this is spec specific testing that will break when we rename the object in the UI
		Record r_obj=spec.getRecord("collection-object");
		assertNotNull(r_obj);
		assertEquals("collection-object",r_obj.getID());
		assertEquals("cataloging",r_obj.getWebURL());
		
		StorageGenerator gen=cspm.getStorage("service");
		CSPRequestCredentials creds=gen.createCredentials();
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,spec.getAdminData().getAuthUser());
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,spec.getAdminData().getAuthPass());
		return gen.getStorage(creds,new RequestCache());
	}
}
