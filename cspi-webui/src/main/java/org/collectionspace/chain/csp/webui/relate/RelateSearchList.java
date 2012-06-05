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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelateSearchList implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RelateSearchList.class);
	private Map<String,String> url_to_typeid=new HashMap<String,String>();
	private boolean search;
	private String searchPath = "main";

	public RelateSearchList(boolean in) { search=in; searchPath="main"; }

	public RelateSearchList(boolean in, String string) {
		search=in;
		searchPath = string;
	}

	private void addRestriction(JSONObject restrictions,String key,String value,boolean map) throws JSONException {
		if(StringUtils.isBlank(value))
			return;
		if(map) {
			String[] in=value.split("/");
			value=url_to_typeid.get(in[0])+"/"+in[1];
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
			JSONObject results = storage.getPathsJSON("relations/"+searchPath,restrictions);
			String[] relations = (String[]) results.get("listItems");
			JSONObject out=new JSONObject();
			JSONArray data=new JSONArray();
			if(searchPath.equals("main")){
				for(String r : relations)
					data.put(r);
				out.put("items",data);
			}
			else{
				if(results.has("listItems")){
					if(results.getJSONObject("moredata").length() >0){
						//there is a relationship
						String[] reld = (String[])results.get("listItems");
						String hcsid = reld[0];
						JSONObject mored = results.getJSONObject("moredata").getJSONObject(hcsid);
						//it's name is
						JSONObject broaderthan = new JSONObject();
						broaderthan.put("label", mored.getString("objectname"));
						out.put("broader", broaderthan);
					}
				}
			}
			request.sendJSONResponse(out);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: ",x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: ",x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: ",x);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		UIRequest uir = q.getUIRequest();
		if(search) {
			search_or_list(q.getStorage(),uir,
					uir.getRequestArgument(RELATION_SOURCE_PARAM),
					uir.getRequestArgument(RELATION_TARGET_PARAM),
					uir.getRequestArgument(RELATION_TYPE_PARAM));
		} else {
			search_or_list(q.getStorage(),uir,null,null,null);
		}
	}

	// XXX refactor these
	public void configure(WebUI ui, Spec spec) {
		for(Record r : spec.getAllRecords()) {
			url_to_typeid.put(r.getWebURL(),r.getID());
		}
	}
}
