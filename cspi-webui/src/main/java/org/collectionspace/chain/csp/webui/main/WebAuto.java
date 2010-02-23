package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.impl.main.ConfigException;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONObject;

public class WebAuto implements WebMethod {
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		q.getUIRequest().sendJSONResponse(new JSONObject());
	}

	public void configure_finish() {}

	public void configure(ReadOnlySection config) throws ConfigException {
		// TODO Auto-generated method stub
		
	}
}
