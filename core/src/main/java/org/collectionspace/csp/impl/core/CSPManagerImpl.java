package org.collectionspace.csp.impl.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.config.main.ConfigFactory;
import org.collectionspace.chain.config.main.impl.MainConfigFactoryImpl;
import org.collectionspace.chain.csp.nconfig.NConfigRoot;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.impl.main.NConfigException;
import org.collectionspace.chain.csp.nconfig.impl.main.RulesImpl;
import org.collectionspace.chain.csp.nconfig.impl.parser.ConfigParser;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UI;
import org.xml.sax.InputSource;

public class CSPManagerImpl implements CSPManager {
	private Set<ConfigProvider> config_providers=new HashSet<ConfigProvider>();
	private Set<ConfigConsumer> config_consumers=new HashSet<ConfigConsumer>();	
	private Set<Configurable> configurable_csps=new HashSet<Configurable>();
	private Set<NConfigurable> nconfig=new HashSet<NConfigurable>();
	private DependencyResolver csps=new DependencyResolver("go");
	private Map<String,StorageGenerator> storage=new HashMap<String,StorageGenerator>();
	private Map<String,UI> ui=new HashMap<String,UI>();
	private NConfigRoot nconfig_root;
	
	public void addConfigProvider(ConfigProvider provider) { config_providers.add(provider); }
	public Set<ConfigProvider> getConfigProviders() { return config_providers; }
	public void addConfigConsumer(ConfigConsumer cfg) { config_consumers.add(cfg); }
	public void addConfigurable(Configurable cfg) { configurable_csps.add(cfg); }
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

	public void runConfigConsumers(final ConfigContext ctx) throws CSPDependencyException {
		DependencyResolver consumers=new DependencyResolver("config-consumers");
		for(final ConfigConsumer consumer : config_consumers) {
			consumers.addRunnable(new Dependable(){
				public void run() throws CSPDependencyException {
					consumer.prepareForConfiguration(ctx);
				}
				public String getName() { return consumer.getName(); }
			});
		}
		consumers.go();
	}
	
	public void configure(InputSource in,String url) throws CSPDependencyException {
		try {
		ConfigFactory cfg_factory=new MainConfigFactoryImpl(this);
		final ConfigRoot configuration=cfg_factory.parseConfig(in,url);
		DependencyResolver configurable=new DependencyResolver("configuration");		
		for(final Configurable config : configurable_csps) {
			configurable.addRunnable(new Dependable(){
				public void run() throws CSPDependencyException {
					config.configure(configuration);
				}
				public String getName() { return config.getName(); }
			});			
		}
		configurable.go();
		} catch(BootstrapConfigLoadFailedException x) { // XXX
			throw new CSPDependencyException(x);
		}
		// // // // //
	}
	
	public void nconfigure(InputSource in,String url) throws CSPDependencyException {
		RulesImpl rules=new RulesImpl();
		for(NConfigurable config : nconfig) {
			config.nconfigure(rules);
		}
		try {
			ConfigParser parser = new ConfigParser(rules);
			parser.parse(in,url);
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
