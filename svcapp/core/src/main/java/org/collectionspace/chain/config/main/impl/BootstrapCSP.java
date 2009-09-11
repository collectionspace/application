/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config.main.impl;

import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigListener;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class BootstrapCSP implements CSP, ConfigConsumer, ConfigProvider {
	private BootstrapConfigController bootstrap;
	
	public BootstrapCSP(BootstrapConfigController bootstrap) { this.bootstrap=bootstrap; }

	public String getName() { return "bootstrap"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigConsumer(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		ctx.addConfigProvider(this);
	}

	public void provide(ConfigListener response) {
		for(String key : bootstrap.getKeys()) {
			response.addConfig(new Object[]{"bootstrap",key},bootstrap.getOption(key));
		}
	}
}
