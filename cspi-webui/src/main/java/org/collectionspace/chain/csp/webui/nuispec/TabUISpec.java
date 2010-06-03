package org.collectionspace.chain.csp.webui.nuispec;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class TabUISpec implements WebMethod {
	private Record record;
	
	public TabUISpec(Record record) {
		this.record=record;
	}
	
	public void configure(WebUI ui, Spec spec) {}

	private JSONObject relatedRecordSpec() throws JSONException {
		return UISpec.generateSidebarPart(record.getUIURL(),false,true, false);
	}
	
	private JSONObject newRecordSpec() throws JSONException {
		JSONObject out=new JSONObject();
		
		for(FieldSet fs : record.getAllFields()) { // XXX does not support repeats, atm
			if(!(fs instanceof Field))
				continue;
			Field field=(Field)fs;
			if(!field.isInTab())
				continue;
			out.put(field.getSelector(),UISpec.plain(field));
		}
		return out;
	}
	
	private JSONObject uispec(UIRequest request,String suffix) throws UIException {
		try {
			JSONObject out=new JSONObject();
			out.put("details",newRecordSpec());
			out.put("list",relatedRecordSpec());
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		JSONObject out=uispec(q.getUIRequest(),StringUtils.join(tail,"/"));
		q.getUIRequest().sendJSONResponse(out);
	}

}
