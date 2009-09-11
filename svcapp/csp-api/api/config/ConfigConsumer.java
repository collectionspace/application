package org.collectionspace.csp.api.config;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface ConfigConsumer {
	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException;
	public String getName();
}
