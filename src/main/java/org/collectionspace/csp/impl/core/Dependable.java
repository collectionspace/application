package org.collectionspace.csp.impl.core;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Dependable {
	public void run() throws CSPDependencyException;
	public String getName();
}
