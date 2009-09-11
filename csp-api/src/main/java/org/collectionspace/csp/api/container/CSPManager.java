package org.collectionspace.csp.api.container;

import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.kludge.BCCKludge;
import org.xml.sax.InputSource;

public interface CSPManager extends CSPContext {
	public void register(CSP in);
	public void go() throws CSPDependencyException;
	public void configure(BCCKludge bootstrap,InputSource in,String url) throws CSPDependencyException;
	public Storage getStorage(String name);
}
