package org.collectionspace.chain.controller;

/* Invoke from command line with recordtype, domain, maketype (core/delta) and configfile
 * 
 * eg java -jar cspace/conf/cspace.jar collectionobjects collectionspace_core core lifesci-tenant.xml
 * 
 */

import org.collectionspace.chain.installation.XsdGeneration;

public class CommandLine {
	public static final void main(String[] args) throws Exception {
		String recordtype = args[0];
		String domain = args[1];
		String maketype = args[2];
		String configfile = args[3];
		XsdGeneration s = new XsdGeneration(configfile, recordtype, domain, maketype);
		String xsdschema = s.getFile();
		System.err.println(xsdschema);
	}
}
