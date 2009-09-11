package org.collectionspace.csp.api.config;

import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.kludge.BCCKludge;
import org.collectionspace.kludge.CRKludge;

public interface Configurable {
	public void configure(BCCKludge bootstrap,CRKludge config) throws CSPDependencyException;
	public String getName();
}
