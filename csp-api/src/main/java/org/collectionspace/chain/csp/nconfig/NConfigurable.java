package org.collectionspace.chain.csp.nconfig;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface NConfigurable {
	public void nconfigure(Rules rules) throws CSPDependencyException;
	public void config_finish() throws CSPDependencyException;
}
