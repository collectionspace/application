/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.container.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.impl.main.RulesImpl;
import org.collectionspace.chain.csp.config.impl.parser.ConfigParser;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UI;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class CSPManagerImpl implements CSPManager {
	private Set<Configurable> config_csps=new HashSet<Configurable>();
	private DependencyResolver csps=new DependencyResolver("go");
	private Map<String,StorageGenerator> storage=new HashMap<String,StorageGenerator>();
	private Map<String,UI> ui=new HashMap<String,UI>();
	private ConfigRoot config_root;
	
	public void addStorageType(String name, StorageGenerator store) { storage.put(name,store); }
	
	public void register(final CSP in) { 
		csps.addRunnable(new Dependable(){
			public void run() throws CSPDependencyException { in.go(CSPManagerImpl.this); }
			public String getName() { return in.getName(); }
		});
	}

	public void go() throws CSPDependencyException {
		csps.go();
	}

	public void configure(InputSource in,EntityResolver er) throws CSPDependencyException {
		RulesImpl rules=new RulesImpl();
		for(Configurable config : config_csps) {
			config.configure(rules);
		}
		try {
			ConfigParser parser = new ConfigParser(rules,er);
			parser.parse(in);
			// Finish up all the config-related tasks
			for(Configurable config : config_csps) {
				config.config_finish();
			}
			// Run the post-config init tasks
			for(Configurable config : config_csps) {
				config.complete_init();
			}
		} catch (ConfigException e) {
			throw new CSPDependencyException(e); // XXX			
		}
	}
	
	public StorageGenerator getStorage(String name) { return storage.get(name); }
	
	public void addUI(String name,UI impl) {
		ui.put(name,impl);
	}
	
	public UI getUI(String name) {
		return ui.get(name);
	}
	public void addConfigRules(Configurable cfg) {
		config_csps.add(cfg);
	}
	public void setConfigRoot(ConfigRoot cfg) { config_root=cfg; }
	public ConfigRoot getConfigRoot() { return config_root; }
}
