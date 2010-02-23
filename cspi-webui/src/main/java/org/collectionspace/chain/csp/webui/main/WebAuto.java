package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONObject;

public class WebAuto implements WebMethod {
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		q.getUIRequest().sendJSONResponse(new JSONObject());
	}

	public void configure(WebUI ui,Spec spec) {}

	public void configure() throws ConfigException {
		// TODO Auto-generated method stub
		
	}
}
