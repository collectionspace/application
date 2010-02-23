package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.impl.main.ConfigException;
import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.ui.UIException;

public interface WebMethod extends TrieMethod {
	public void configure(ReadOnlySection config) throws ConfigException;
	public void configure_finish();
	public void run(Object in,String[] tail) throws UIException;
}
