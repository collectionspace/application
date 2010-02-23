/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigListener;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;

public class BootstrapCSP implements CSP, ConfigConsumer, ConfigProvider, NConfigurable {
	public static final String SECTION_PREFIX="org.collectionspace.app.config.bootstrap.";	
	public static String BOOTSTRAP_ROOT=SECTION_PREFIX+"bootstrap";
	private BootstrapConfigController bootstrap;
	
	public BootstrapCSP(BootstrapConfigController bootstrap) { this.bootstrap=bootstrap; }

	public String getName() { return "bootstrap"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigConsumer(this);
		ctx.addConfigRules(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		ctx.addConfigProvider(this);
	}

	public void provide(ConfigListener response) {
		for(String key : bootstrap.getKeys()) {
			response.addConfig(new Object[]{"bootstrap",key},bootstrap.getOption(key));
		}
	}

	public void nconfigure(Rules rules) {
		/* MAIN/bootstrap -> BOOTSTRAP */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"bootstrap"},SECTION_PREFIX+"bootstrap",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(BOOTSTRAP_ROOT,bootstrap);
				return bootstrap;
			}
		});
	}

	public void config_finish() {}
}
