/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.userroles;

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


public class UserRolesRead implements WebMethod{
	private static final Logger log=LoggerFactory.getLogger(UserRolesRead.class);
	private String base,base_url,sub_base;
	private boolean record_type;
	
	public UserRolesRead(Record r){
		this.base = r.getID();
		this.base_url = r.getServicesURL();
		this.sub_base = r.getSpec().getRecord("users").getID();
		record_type=r.isType("authorizationdata");
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			if(!record_type) {
				JSONObject fields=storage.retrieveJSON("base+/"+csid, new JSONObject());
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				out.put("fields",fields);
				out.put("isError",false);
				JSONObject messages = new JSONObject();
				messages.put("message", "");
				messages.put("severity", "info");
				JSONArray arr = new JSONArray();
				arr.put(messages);
				out.put("messages", arr);
				out.put("relations",new JSONArray());
			} else {
				out=storage.retrieveJSON(this.sub_base+"/"+csid, new JSONObject());
			}
		} catch (ExistException e) {
			throw new UIException("ExistException "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		} catch (JSONException e) {
			throw new UIException("Could not create JSON"+e,e);
		}
		if (out == null) {
			throw new UIException("No JSON Found");
		}
		return out;
	}
	
	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		// Get the data
		JSONObject outputJSON = getJSON(storage,path);
		try {
			outputJSON.put("csid",path);
		} catch (JSONException e1) {
			throw new UIException("Cannot add csid",e1);
		}
		// Write the requested JSON out
		request.sendJSONResponse(outputJSON);
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q = (Request)in;
		store_get(q.getStorage(),q.getUIRequest(), StringUtils.join(tail,"/"));
		
	}
	
	public void configure(WebUI ui, Spec spec) {
	}
}
