package org.collectionspace.chain.controller;

import org.collectionspace.chain.installation.XsdGeneration;
import org.collectionspace.csp.api.ui.UIException;

public class CommandLine {
	public static final void main(String[] args) throws Exception {
		String recordtype = "collectionobjects";
		String domain = "collectionspace_core";
		String maketype = "core";
		String configfile = "lifesci-tenant.xml";
		XsdGeneration s = new XsdGeneration(configfile, recordtype, domain, maketype);
		String xsdschema = s.getFile();
		System.err.println(xsdschema);
	}
}
