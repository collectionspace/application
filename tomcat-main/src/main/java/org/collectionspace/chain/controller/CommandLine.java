package org.collectionspace.chain.controller;

/* Invoke from command line with recordtype, domain, maketype (core/delta) and configfile
 * 
 * configfile is the config for the tenant in question.
 * record is the type of the record according to its name in the URL of the service (eg "collectionobjects")
 * domain is the section of the XML to generate the XSD for (eg "collectionspace_core")
 * maketype should be core. The other value, delta, is experimental.
 * 
 * eg java -jar cspace/conf/cspace.jar collectionobjects collectionspace_core core lifesci-tenant.xml
 * 
 */

import java.util.HashMap;

import org.collectionspace.chain.installation.XsdGeneration;

public class CommandLine {
	public static final void main(String[] args) throws Exception {
		String recordtype = "intakes";//args[0];
		String domain = "collectionspace_core"; //args[1];
		String maketype = "core"; //args[2];
		String configfile = "core-tenant.xml"; //args[3];
		
		// Generate all the Service schemas from the Application layer's configuration records
		XsdGeneration s = new XsdGeneration(configfile, domain, maketype, "3.0");
		HashMap<String, String> xsdschemas = s.getServiceSchemas();
		
		System.out.println(String.format("Record type: %s", recordtype));
		for (String schemaName : xsdschemas.keySet()) {
			System.out.println(String.format("\tSchema file name: %s", schemaName));
			//System.out.println(xsdschemas.get(schemaName));
		}
	}
}
