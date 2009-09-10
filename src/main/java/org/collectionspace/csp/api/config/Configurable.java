package org.collectionspace.csp.api.config;

import org.collectionspace.chain.config.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.config.main.ConfigRoot;
import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Configurable {
	public void configure(BootstrapConfigController bootstrap,ConfigRoot config) throws CSPDependencyException;
	public String getName();
}
