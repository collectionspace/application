package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.ui.UIException;

public interface WebMethod extends TrieMethod {
	public void configure(WebUI ui,Spec spec);
	public void run(Object in,String[] tail) throws UIException;
}
