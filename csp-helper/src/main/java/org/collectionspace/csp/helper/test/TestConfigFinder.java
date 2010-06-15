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
	public static final String xxx_servicesBaseURL = "http://nightly.collectionspace.org:8180"; // XXX hard-wired - ought to be found from the config file!
	
	public static InputStream getConfigStream() throws CSPDependencyException {
		// TODO next stage will be to move default.xml into here and rename it (CSPACE-1288)
		// CSPACE-2114 initial (still messy, but better than before) stage is to change 
		// from 3 files (2x config and default) to just one of them (default.xml hard-coded here)
		String classNearDefaultXml = "org.collectionspace.chain.controller.ChainServlet";
		String configFilename = "default.xml";
		try {
			InputStream result = Class.forName(classNearDefaultXml).getClassLoader().getResourceAsStream(configFilename);
			if(result!=null) {
				log.info("Found config for testing: "+configFilename);
				return result;
			} else {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			log.info("Failed to find config for unit testing");
			throw new CSPDependencyException("Failed to find a config file for unit testing");
		}
	}
}
