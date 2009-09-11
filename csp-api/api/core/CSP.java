package org.collectionspace.csp.api.core;

public interface CSP {
	public void go(CSPContext ctx) throws CSPDependencyException;
	public String getName();
}
