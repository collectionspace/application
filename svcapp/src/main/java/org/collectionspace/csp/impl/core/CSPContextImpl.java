package org.collectionspace.csp.impl.core;

import java.util.HashSet;
import java.util.Set;

import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPManager;

public class CSPContextImpl implements CSPManager {
	private Set<ConfigProvider> config_providers=new HashSet<ConfigProvider>();
	private Set<ConfigConsumer> config_consumers=new HashSet<ConfigConsumer>();	
	private DependencyResolver csps=new DependencyResolver("go");

	public void addConfigProvider(ConfigProvider provider) { config_providers.add(provider); }
	public Set<ConfigProvider> getConfigProviders() { return config_providers; }
	public void addConfigConsumer(ConfigConsumer cfg) { config_consumers.add(cfg); }

	public void register(final CSP in) { 
		csps.addRunnable(new Dependable(){
			public void run() throws CSPDependencyException { in.go(CSPContextImpl.this); }
			public String getName() { return in.getName(); }
		});
	}

	public void go() throws CSPDependencyException {
		csps.go();
	}

	public void runConfigConsumers(final ConfigContext ctx) throws CSPDependencyException {
		DependencyResolver resolver=new DependencyResolver("config-consumers");
		for(final ConfigConsumer consumer : config_consumers) {
			resolver.addRunnable(new Dependable(){
				public void run() throws CSPDependencyException {
					consumer.prepareForConfiguration(ctx);
				}
				public String getName() { return consumer.getName(); }
			});
		}
		resolver.go();
	}
}
