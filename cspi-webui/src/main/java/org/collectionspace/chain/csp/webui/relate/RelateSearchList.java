package org.collectionspace.chain.csp.webui.relate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RelateSearchList implements WebMethod {
	private Map<String,String> url_to_type=new HashMap<String,String>();
	private boolean search;

	public RelateSearchList(boolean in) { search=in; }

	private void addRestriction(JSONObject restrictions,String key,String value,boolean map) throws JSONException {
		if(StringUtils.isBlank(value))
			return;
		if(map) {
			String[] in=value.split("/");
			value=url_to_type.get(in[0])+"/"+in[1];
		}
		restrictions.put(key,value);	
	}

	private void search_or_list(Storage storage,UIRequest request,String source,String target,String type) throws UIException {
		try {
			JSONObject restrictions=new JSONObject();
			addRestriction(restrictions,"src",source,true);
			addRestriction(restrictions,"dst",target,true);
			addRestriction(restrictions,"type",type,false);
			// XXX CSPACE-1834 need to support pagination
			String[] relations = storage.getPaths("relations/main",restrictions);
			JSONObject out=new JSONObject();
			JSONArray data=new JSONArray();
			for(String r : relations)
				data.put(r);
			out.put("items",data);
			request.sendJSONResponse(out);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new UIException("Problem storing: "+x,x);
		}
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("source"),
					q.getUIRequest().getRequestArgument("target"),q.getUIRequest().getRequestArgument("type"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null,null,null);
	}

	// XXX refactor these
	public void configure(WebUI ui, Spec spec) {
		for(Record r : spec.getAllRecords()) {
			url_to_type.put(r.getWebURL(),r.getID());
		}
	}
}
