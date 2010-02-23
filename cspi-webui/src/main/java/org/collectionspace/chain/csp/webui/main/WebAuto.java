package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONObject;

public class WebAuto implements WebMethod {
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		q.getUIRequest().sendJSONResponse(new JSONObject());
	}

	public void configure(ReadOnlySection config) throws ConfigException {}
	public void configure_finish() {}
}
