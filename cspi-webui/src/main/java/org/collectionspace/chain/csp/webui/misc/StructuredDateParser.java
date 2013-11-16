package org.collectionspace.chain.csp.webui.misc;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class StructuredDateParser implements WebMethod {

	@Override
	public void configure(WebUI ui, Spec spec) {
		// Intentionally left blank.
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {
		UIRequest request = ((Request) in).getUIRequest();
		String dateString = request.getRequestArgument("date");

		// DO IT
		
		JSONObject output = new JSONObject();

		try {
			output.put("input", dateString);
		}
		catch(JSONException e) {
			throw new UIException("Error", e);
		}
		
		request.sendJSONResponse(output);
	}
}
