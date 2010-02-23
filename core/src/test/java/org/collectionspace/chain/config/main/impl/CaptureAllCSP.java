/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config.main.impl;

import java.io.File;
import java.io.IOException;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.persistence.ProxyStorage;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class CaptureAllCSP implements CSP, ConfigConsumer {
	private String root;

	public CaptureAllCSP() {}

	public String getStoreRoot() { return root; }

	public String getName() { return "capture-all"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigConsumer(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		SimpleConfigProviderBarbWirer all=new SimpleConfigProviderBarbWirer(new Object[]{"all"});		
		ctx.getRootBarbWirer().getBarb("root").attach(all,"all");
		ctx.addConfigProvider(all);
	}
}
