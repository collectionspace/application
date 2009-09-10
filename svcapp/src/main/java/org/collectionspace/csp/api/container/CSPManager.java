package org.collectionspace.csp.api.container;

import org.collectionspace.chain.config.api.ConfigLoadFailedException;
import org.collectionspace.chain.config.bootstrap.BootstrapConfigController;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.xml.sax.InputSource;

public interface CSPManager extends CSPContext {
	public void register(CSP in);
	public void go() throws CSPDependencyException;
	public void configure(BootstrapConfigController bootstrap,InputSource in,String url) throws ConfigLoadFailedException, CSPDependencyException;
	public Storage getStorage(String name);
}
