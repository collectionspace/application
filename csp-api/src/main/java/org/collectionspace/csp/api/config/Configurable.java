package org.collectionspace.csp.api.config;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Configurable {
	public void configure(ConfigRoot config) throws CSPDependencyException;
	public String getName();
}
