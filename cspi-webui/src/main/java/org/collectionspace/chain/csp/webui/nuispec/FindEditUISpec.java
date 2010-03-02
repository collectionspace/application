package org.collectionspace.chain.csp.webui.nuispec;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FindEditUISpec implements WebMethod {
	private Record[] records;
	
	public FindEditUISpec(Record[] records) {
		this.records=records;
	}
	
	public void configure(WebUI ui, Spec spec) {}

	private JSONObject listEntry(String selector,String number_selector,String page) throws JSONException {
		JSONObject number=new JSONObject();
		number.put("linktext","${items.0.number}");
		number.put("target",page+"?csid=${items.0.csid}");
		JSONObject child=new JSONObject();
		child.put(number_selector,number);
		child.put(".csc-summary","${items.0.summary}");
		JSONArray children=new JSONArray();
		children.put(child);
		JSONObject value=new JSONObject();
		value.put("children",children);
		JSONObject out=new JSONObject();
		out.put(selector,value);
		return out;
	}
	
	private JSONObject uispec(UIRequest request,String suffix) throws UIException {
		try {
			JSONObject out=new JSONObject();
			for(Record record : records) {
				if(!record.isInFindEdit())
					continue;
				out.put(record.getListKey(),listEntry(record.getRowSelector(),record.getNumberSelector(),record.getUIURL()));				
			}
			out.put("authorityTerms",new JSONObject());
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
