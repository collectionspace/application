/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfigFinder {

	private static final Logger log=LoggerFactory.getLogger(TestConfigFinder.class);
	
	// This method only works for Eclipse and not mvn test :( - see below
	private static final String classNearDefaultXml = "org.collectionspace.chain.controller.ChainServlet"; 
	
	//used to test multi tenancy
	public static InputStream getConfigStream(String filename) throws CSPDependencyException {
		InputStream out=getConfigStreamViaEnvironmentVariable();
		if(out!=null)
			return out;		
		out=getConfigStreamViaClassLoader(filename);
		if(out!=null)
			return out;
		throw new CSPDependencyException("No config file found by any method");
	}

	public static InputStream getConfigStream() throws CSPDependencyException {
		return getConfigStream("default.xml");
	}

	private static InputStream getConfigStreamViaEnvironmentVariable() throws CSPDependencyException {
		Map<String,String> env_vars=System.getenv();
		if(env_vars==null)
			return null;
		String filename=env_vars.get("TEST_CONFIG");
		if(filename==null)
			return null;
		try {
			InputStream file = new FileInputStream(filename);
			if(file==null)
				throw new CSPDependencyException("Could not open specified file in TEST_CONFIG: "+filename);
			return file;
		} catch (FileNotFoundException e) {
			throw new CSPDependencyException("Could not open specified file in TEST_CONFIG: "+filename);
		}
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
}
