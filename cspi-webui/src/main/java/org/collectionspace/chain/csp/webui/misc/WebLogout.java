/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;

public class WebLogout implements WebMethod {
	private String front_page;
	
	public void logout(Request in) throws UIException {
		UIRequest request=in.getUIRequest();
		request.getSession().setValue(UISession.USERID,"");
		request.getSession().setValue(UISession.PASSWORD,"");
		in.reset();

		request.setRedirectPath(front_page.split("/"));
	}
	
	public void run(Object in,String[] tail) throws UIException {
		logout((Request)in);
	}

	public void configure(WebUI ui, Spec spec) {
		front_page=ui.getFrontPage();
	}
}
