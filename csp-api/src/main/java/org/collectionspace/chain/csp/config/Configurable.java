package org.collectionspace.chain.csp.config;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Configurable {
	public void configure(Rules rules) throws CSPDependencyException;
	public void config_finish() throws CSPDependencyException;
}
