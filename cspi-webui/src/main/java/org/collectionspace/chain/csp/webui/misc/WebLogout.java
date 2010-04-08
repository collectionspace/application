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
	
	public void run(Object in, String[] tail) throws UIException {
		UIRequest request=((Request)in).getUIRequest();
		request.getSession().setValue(UISession.USERID,"");
		request.getSession().setValue(UISession.PASSWORD,"");
		((Request)in).reset();
		request.setRedirectPath(front_page.split("/"));
	}

	public void configure(WebUI ui, Spec spec) {
		front_page=ui.getFrontPage();
	}
}
