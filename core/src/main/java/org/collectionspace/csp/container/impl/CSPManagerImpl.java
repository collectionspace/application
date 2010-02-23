package org.collectionspace.csp.container.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.nconfig.NConfigRoot;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.impl.main.NConfigException;
import org.collectionspace.chain.csp.nconfig.impl.main.RulesImpl;
import org.collectionspace.chain.csp.nconfig.impl.parser.ConfigParser;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UI;
import org.xml.sax.InputSource;

public class CSPManagerImpl implements CSPManager {
	private Set<NConfigurable> nconfig=new HashSet<NConfigurable>();
	private DependencyResolver csps=new DependencyResolver("go");
	private Map<String,StorageGenerator> storage=new HashMap<String,StorageGenerator>();
	private Map<String,UI> ui=new HashMap<String,UI>();
	private NConfigRoot nconfig_root;
	
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

	public void nconfigure(InputSource in,String url) throws CSPDependencyException {
		RulesImpl rules=new RulesImpl();
		for(NConfigurable config : nconfig) {
			config.nconfigure(rules);
		}
		try {
			ConfigParser parser = new ConfigParser(rules);
			parser.parse(in,url);
			for(NConfigurable config : nconfig) {
				config.config_finish();
			}
		} catch (NConfigException e) {
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
	public void addConfigRules(NConfigurable cfg) {
		nconfig.add(cfg);
	}
	public void setNConfigRoot(NConfigRoot cfg) { nconfig_root=cfg; }
	public NConfigRoot getNConfigRoot() { return nconfig_root; }
}
