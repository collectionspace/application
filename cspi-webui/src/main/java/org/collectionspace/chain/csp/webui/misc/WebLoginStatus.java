package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONException;
import org.json.JSONObject;

public class WebLoginStatus  implements WebMethod {
	
	public void testlogin(Request in) throws UIException {
		JSONObject output= new JSONObject();
		UIRequest request=in.getUIRequest();
		try {
			if(request.getSession() != null && request.getSession().getValue(UISession.USERID) != null ){
				if(request.getSession().getValue(UISession.USERID).equals("")){
					output.put("login", false);
				}				
				else{
					output.put("login", true);
				}
			}
			else{
				output.put("login", false);
			}
			request.sendJSONResponse(output);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		}
	}
	
	public void run(Object in,String[] tail) throws UIException {
		testlogin((Request)in);
	}

	public void configure(WebUI ui, Spec spec) {
		
	}
}
