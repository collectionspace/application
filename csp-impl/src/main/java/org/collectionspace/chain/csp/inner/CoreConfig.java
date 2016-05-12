/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.inner;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.RuleSet;
import org.collectionspace.chain.csp.config.RuleTarget;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;

// FIXME: Call order DependencyNotSatisfiedException
public class CoreConfig implements CSP, Configurable, ConfigRoot {
	private Map<String, Object> roots = new HashMap<String, Object>();
	public static String SECTIONED = "org.collectionspace.app.config.spec";

	@Override
	public void go(CSPContext ctx) {
		ctx.addConfigRules(this);
		ctx.setConfigRoot(this);
		ctx.addConfigRules(this);
	}

	@Override
	public String getName() {
		return "config.core";
	}

	@Override
	public void configure(RuleSet rules) {

		rules.addRule("ROOT", new String[] { "collection-space" },
				"org.collectionspace.app.cfg.main", null, new RuleTarget() {
					@Override
					public Object populate(Object parent, ReadOnlySection milestone) {
						return this;
					}
				});
		
		rules.addRule("org.collectionspace.app.cfg.main",
				new String[] { "cspace-config" }, SECTIONED, null,
				new RuleTarget() {
					@Override
					public Object populate(Object parent, ReadOnlySection section) {
						return CoreConfig.this;
					}
				});
		/* ROOT/collection-space -> MAIN */

	}

	@Override
	public void setRoot(String key, Object value) {
		roots.put(key, value);
	}

	@Override
	public Object getRoot(String key) {
		return roots.get(key);
	}

	@Override
	public void config_finish() {
		// Intentionally blank
	}
	
	@Override
	public void complete_init(CSPManager cspManager, boolean forXsdGeneration) {
		// Intentionally blank
	}
}
