package org.collectionspace.csp.api.config;

public interface ConfigContext {
	public BarbWirer getRootBarbWirer();
	public void addConfigProvider(ConfigProvider provider);
}
