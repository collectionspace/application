/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;

public class BootstrapCSP implements CSP, Configurable {
	public static final String SECTION_PREFIX="org.collectionspace.app.config.bootstrap.";	
	public static String BOOTSTRAP_ROOT=SECTION_PREFIX+"bootstrap";
	private BootstrapConfigController bootstrap;
	
	public BootstrapCSP(BootstrapConfigController bootstrap) { this.bootstrap=bootstrap; }

	public String getName() { return "bootstrap"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	public void configure(Rules rules) {
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
