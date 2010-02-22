package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.ui.UIException;

public interface WebMethod extends TrieMethod {
	public void configure(ConfigRoot config) throws ConfigException;
	public void run(Object in,String[] tail) throws UIException;
}
