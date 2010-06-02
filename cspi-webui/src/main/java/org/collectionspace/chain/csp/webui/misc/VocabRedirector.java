package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabRedirector implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(VocabRedirector.class);
	private Record r;
	
	public VocabRedirector(Record r) { this.r=r; }
	
	public void configure(WebUI ui, Spec spec) {}

	private String pathFor(String in) {
		Field fd = (Field) r.getRepeatField(in);
		String weburl = fd.getAutocompleteInstance().getWebURL();
		return "/vocabularies/"+weburl; 
	}
	
	private void redirect(CSPRequestCache cache,Storage storage,UIRequest request,String[] tail) throws UIException {
		try {
			JSONObject out=new JSONObject();
			out.put("url",pathFor(tail[0]));
			request.sendJSONResponse(out);
		} catch (JSONException e) {
			throw new UIException("JSON building failed",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		redirect(q.getCache(),q.getStorage(),q.getUIRequest(),tail);
	}

}
