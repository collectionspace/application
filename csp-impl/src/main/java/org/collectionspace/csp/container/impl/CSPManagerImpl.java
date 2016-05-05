/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.container.impl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.impl.main.RuleSetImpl;
import org.collectionspace.chain.csp.config.impl.parser.ConfigParser;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class CSPManagerImpl implements CSPManager {
	private static final Logger log=LoggerFactory.getLogger(CSPManagerImpl.class);

	private Set<Configurable> config_csps = new HashSet<Configurable>();
	private DependencyResolver cspDependencyResolver = new DependencyResolver("go"); // The magic "go" command of a CSP. What is a CSP?  Instances of CSP are: CoreConfig, FileStorage, ServicesStorageGenerator, Spec, and WebUI.
	private Map<String,StorageGenerator> storage = new HashMap<String,StorageGenerator>();
	private Map<String,UI> ui=new HashMap<String,UI>();
	private ConfigRoot config_root;
	private File configBase;
	
	public CSPManagerImpl() {
		log.trace("Created another instance of CSPManagerImpl class.");
		configBase = null;
	}
		
	@Override
	public void addStorageType(String name, StorageGenerator store) {
		storage.put(name,store);
	}
	
	@Override
	public void register(final CSP in) { 
		cspDependencyResolver.addRunnable(new Dependable() { // Creates an anonymous type/class that implements Dependable interface
			@Override
			public void run() throws CSPDependencyException {
				in.go(CSPManagerImpl.this);
			}
			
			@Override
			public String getName() {
				return in.getName();
			}
		});
	}

	@Override
	public void go() throws CSPDependencyException {
		cspDependencyResolver.go();
	}

	@Override
	public void configure(InputSource in, EntityResolver er, boolean forXsdGeneration) throws CSPDependencyException {
		RuleSetImpl rules=new RuleSetImpl();
		for (Configurable config : config_csps) {
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
			for (Configurable config : config_csps) {
				config.complete_init(this, forXsdGeneration);
			}
		} catch (ConfigException e) {
			throw new CSPDependencyException(e); // XXX			
		}
	}
	
	@Override
	public void setConfigBase(File file) {
		configBase = file;
	}
	
	@Override	
	public File getConfigBase() {
		return configBase;
	}
	
	@Override
	public StorageGenerator getStorage(String name) {
		return storage.get(name);
	}
	
	@Override
	public void addUI(String name,UI impl) {
		ui.put(name,impl);
	}
	
	@Override
	public UI getUI(String name) {
		return ui.get(name);
	}
	
	@Override
	public void addConfigRules(Configurable cfg) {
		config_csps.add(cfg);
	}
	
	@Override
	public void setConfigRoot(ConfigRoot cfg) {
		config_root=cfg;
	}
	
	@Override
	public ConfigRoot getConfigRoot() {
		return config_root;
	}
}
