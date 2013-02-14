/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;

public interface WebMethod extends TrieMethod {
	public final static String WORKFLOW_SIMPLE_SUB_RESOURCE = "/workflow";
	public final static String WORKFLOW_SUB_RESOURCE = "/*/workflow/";
	public static final String AUTO_COMPLETE_QUERY_PARAM = "q";
	public static final String SEARCH_QUERY_PARAM = "query";
	public static final String MARK_RELATED_QUERY_PARAM = "mkRtSbj";
	public static final String CONSTRAIN_VOCAB_PARAM = "vocab";
	public static final String PAGE_SIZE_PARAM = "pageSize";
	public static final String PAGE_NUM_PARAM = "pageNum";
	public static final String USERID_PARAM = "userid";
	public static final String PASSWORD_PARAM = "password";
	public static final String RELATION_SOURCE_PARAM = "source";
	public static final String RELATION_TARGET_PARAM = "target";
	public static final String RELATION_TYPE_PARAM = "type";
	public static final String RELATION_ONE_WAY_PARAM = "one-way";

	public void configure(WebUI ui,Spec spec);
	@Override
	public void run(Object in,String[] tail) throws UIException;
}
