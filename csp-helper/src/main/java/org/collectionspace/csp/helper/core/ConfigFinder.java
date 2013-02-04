/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.collectionspace.csp.api.core.CSPDependencyException;

/** Here's where we look for the config. 
 * 
 * First we look for an environment variable containing the actual config.
 * Next we look for a servlet attribute specifying a path.
 * Next we look in ${jboss.home.dir}/server/cspace/conf/cspace-config.xml
 * Next we look in the classpath for a name supplied in a servlet attribute.
 * Next we look in the classpath for something called cspace-config.xml
 * (Tomcat 5  = $TOMCAT_HOME/shared/classes
 * Tomcat 6 = $TOMCAT_HOME/lib)
 * Finally we fail.
 * 
 * @author dan
 *
 */
public class ConfigFinder implements EntityResolver {
	private static final Logger log=LoggerFactory.getLogger(ConfigFinder.class);
	private static final String classNearDefaultXml = "org.collectionspace.chain.controller.ChainServlet"; 

	private ServletContext ctx;

	public ConfigFinder(ServletContext ctx) {
		this.ctx=ctx;
	}

	private InputStream getDataFromAttribute() {
		String out=(String)ctx.getAttribute("config-data");
		if(out==null)
			return null;
		return IOUtils.toInputStream(out);
	}

	private InputStream getDataFromAttributePath() throws IOException {
		String out=(String)ctx.getAttribute("config-path");
		if(out==null)
			return null;
		File file=new File(out);
		if(!file.exists())
			return null;
		return new FileInputStream(file);
	}

	private InputStream getDataFromName() {
		String path=(String)ctx.getAttribute("config-filename");

		try {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		} catch(Exception x) { return null; }
	}

	@SuppressWarnings("unchecked")
	private static InputStream getDataFromJBossPath(String filename) {
		try {
			File file=new File(System.getProperty("jboss.home.dir")+"/server/cspace/conf/"+filename);
			Properties p=System.getProperties();
			for(Entry k : p.entrySet()) {
				log.debug(" property "+k.getKey()+" = "+k.getValue());
			}
			log.debug("A Looking in "+System.getProperty("jboss.home.dir")+"/server/cspace/conf/"+filename);
			if(!file.exists())
				return null;
			return new FileInputStream(file);			
		} catch(Exception x) { return null; }
	}

	private static InputStream getDataFromEnvironmentVariable() {
		try {
			String filename=System.getenv("TEST_CONFIG");
			if(filename==null)
				return null;
			File file=new File(filename);
			if(!file.exists())
				return null;
			return new FileInputStream(file);			
		} catch(Exception x) { return null; }
	}

	private static InputStream getDataFromClasspath(String filename) {
		try {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		} catch(Exception x) { return null; }
	}

	private static InputStream getConfigStreamViaClassLoader(String configFilename) throws CSPDependencyException {
		// TODO next stage will be to move default.xml into here and rename it (CSPACE-1288)
		// CSPACE-2114 initial (still messy, but better than before) stage is to change 
		// from 3 files (2x config and default) to just one of them (default.xml hard-coded here)

		ClassLoader loader;
		try {
			// In Eclipse all classes are still visible so we can jump across via a known class name
			loader = Class.forName(classNearDefaultXml).getClassLoader();
		} catch (ClassNotFoundException e) {
			// In Maven we can only see stuff in target/test-classes
			// so this relies on top-level pom.xml copying the file in for us 
			log.debug("Falling back to current thread ClassLoader");
			loader = Thread.currentThread().getContextClassLoader();
		}
		try {
			InputStream result = loader.getResourceAsStream(configFilename);
			if(result!=null) {
				log.debug("Found config for testing: "+configFilename);
				return result;
			} else {
				throw new CSPDependencyException("Failed to open config file at "+configFilename);
			}
		} catch (Exception e) {
			return null;
		}
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		try {
			InputStream out=getDataFromEnvironmentVariable();
			if(out!=null)
				return new InputSource(out);
			if(ctx!=null && "-//CSPACE//ROOT".equals(publicId)) {
				if(out!=null)
					return new InputSource(out);		
				out=getDataFromAttribute();
				if(out!=null)
					return new InputSource(out);
				out=getDataFromAttributePath();
				if(out!=null)
					return new InputSource(out);
				out=getDataFromName();
				if(out!=null)
					return new InputSource(out);
			
			}
//use config from tomcat-main/src/main/resources if this is a test run by mvn
			if("-//CSPACE//TESTROOT".equals(publicId)){
				out=getConfigStreamViaClassLoader(systemId);
				if(out!=null)
					return new InputSource(out);		
			}
			out=getDataFromJBossPath(systemId);
			if(out!=null)
				return new InputSource(out);
			out=getDataFromClasspath(systemId); // running tests find the resource/entity here
			if(out!=null)
				return new InputSource(out);
			out=getConfigStreamViaClassLoader(systemId);
			if(out!=null)
				return new InputSource(out);		
			throw new SAXException("No such file "+systemId);
		} catch(CSPDependencyException e) {
			throw new SAXException("Error parsing",e);		
		}
	}
}
