package org.collectionspace.csp.impl.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.config.main.ConfigFactory;
import org.collectionspace.chain.config.main.impl.MainConfigFactoryImpl;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.xml.sax.InputSource;

public class CSPManagerImpl implements CSPManager {
	private Set<ConfigProvider> config_providers=new HashSet<ConfigProvider>();
	private Set<ConfigConsumer> config_consumers=new HashSet<ConfigConsumer>();	
	private Set<Configurable> configurable_csps=new HashSet<Configurable>();	
	private DependencyResolver csps=new DependencyResolver("go");
	private Map<String,Storage> storage=new HashMap<String,Storage>();
	
	public void addConfigProvider(ConfigProvider provider) { config_providers.add(provider); }
	public Set<ConfigProvider> getConfigProviders() { return config_providers; }
	public void addConfigConsumer(ConfigConsumer cfg) { config_consumers.add(cfg); }
	public void addConfigurable(Configurable cfg) { configurable_csps.add(cfg); }
	public void addStorageType(String name, Storage store) { storage.put(name,store); }
	
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
	}

	public Storage getStorage(String name) { return storage.get(name); }
	
}
