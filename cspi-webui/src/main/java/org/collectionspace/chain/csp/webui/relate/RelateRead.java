/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
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
import org.json.JSONException;
import org.json.JSONObject;

public class RelateRead implements WebMethod {
	private Map<String,String> type_to_url=new HashMap<String,String>();
	private Map<String,String> servicename_to_serviceid=new HashMap<String,String>();
	private String searchPath = "main";

	public RelateRead(String string) {
		searchPath = string;
	}
	public RelateRead() {
		searchPath = "main";
	}

	private JSONObject createMiniRecord(Storage storage,String type,String csid) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view", new JSONObject());
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;		
	}
	
	/* Note that one-way is only valid on create/update */
	private JSONObject convertPayload(Storage storage,JSONObject in,String path) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		String[] src=in.getString("src").split("/");
		String[] dst=in.getString("dst").split("/");
		out.put("source",createMiniRecord(storage,servicename_to_serviceid.get(src[0]),src[1]));
		out.put("target",createMiniRecord(storage,servicename_to_serviceid.get(dst[0]),dst[1]));
		out.put("type",in.get("type"));
		out.put("csid",path);
		return out;
	}
	
	private void relate_get(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject relation=convertPayload(storage,storage.retrieveJSON("/relations/"+searchPath+"/"+path, new JSONObject()),path);
			request.sendJSONResponse(relation);
		} catch (ExistException e) {
			throw new UIException("JSON Not found ",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		} catch (JSONException e) {
			throw new UIException("Could not build JSON ",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		relate_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
			servicename_to_serviceid.put(r.getServicesTenantSg(), r.getID());
		}
	}
}
