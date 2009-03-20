package org.collectionspace.xxu.api;

import org.dom4j.Node;

public interface CSPProviderFactory {
	public CSPProvider process(CSP csp,Node in) throws ConfigLoadingException;
}
