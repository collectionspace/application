/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.test;

import java.io.InputStream;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This probably duplicates some of the functionality
 * of bconfig-utils and/or tomcat-main's ConfigFinder
 * but this is deliberately very simple, and should work
 * in JUnit tests without any other dependencies.
 * Its scope is intended to be restricted to use in JUnit tests ONLY.
 */
public class TestConfigFinder {

	private static final Logger log=LoggerFactory.getLogger(TestConfigFinder.class);
	
	// xxx_servicesBaseURL moved here from ServicesBaseClass as a staging point before we get rid of it
	public static final String xxx_servicesBaseURL = "http://localhost:8180"; // only used if everything else fails.
	public static final String configFilename = "default.xml";
	
	// This method only works for Eclipse and not mvn test :( - see below
	private static final String classNearDefaultXml = "org.collectionspace.chain.controller.ChainServlet"; 
	
	public static InputStream getConfigStream() throws CSPDependencyException {
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
				throw new NullPointerException();
			}
		} catch (Exception e) {
			throw new CSPDependencyException("Failed to find a config file for unit testing");
		}
	}
}
