/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.ui.UIException;

public interface WebMethod extends TrieMethod {
	public void configure(WebUI ui,Spec spec);
	public void run(Object in,String[] tail) throws UIException;
}
