package org.collectionspace.csp.container.impl;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Dependable {
	public void run() throws CSPDependencyException;
	public String getName();
}
