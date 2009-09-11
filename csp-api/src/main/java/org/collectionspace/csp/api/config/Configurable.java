package org.collectionspace.csp.api.config;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.kludge.CRKludge;

public interface Configurable {
	public void configure(CRKludge config) throws CSPDependencyException;
	public String getName();
}
