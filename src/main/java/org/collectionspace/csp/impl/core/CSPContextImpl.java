package org.collectionspace.csp.impl.core;

import java.util.HashSet;
import java.util.Set;

import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.core.CSPContext;

public class CSPContextImpl implements CSPContext {
	private Set<ConfigProvider> config_providers=new HashSet<ConfigProvider>();
	private Set<ConfigConsumer> config_consumers=new HashSet<ConfigConsumer>();	
	
	public void addConfigProvider(ConfigProvider provider) { config_providers.add(provider); }
	public Set<ConfigProvider> getConfigProviders() { return config_providers; }
	public void addConfigConsumer(ConfigConsumer cfg) { config_consumers.add(cfg); }
	public Set<ConfigConsumer> getConfigConsumers() { return config_consumers; }	
}
