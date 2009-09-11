package org.collectionspace.csp.api.config;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Configurable {
	public void configure(Object bootstrap,Object config) throws CSPDependencyException;
	public String getName();
}
