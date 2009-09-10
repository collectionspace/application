package org.collectionspace.csp.api.core;

public interface CSPManager extends CSPContext {
	public void register(CSP in);
	public void go() throws CSPDependencyException;
}
