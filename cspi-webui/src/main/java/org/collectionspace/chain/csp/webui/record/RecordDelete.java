/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.record;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
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


public class RecordDelete implements WebMethod {
	private String base;
	
	public RecordDelete(String base) { this.base=base; }

	private void store_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			if(base.equals("role")){
				//business logic. Only delete role if no active users exists who have this role set
				//CSPACE-3283
				// Note that given this, we need not clear the userperms cache when deleting a role.

				String url = base+"/"+path+"/"+"accountroles/";
				JSONObject accounts = storage.retrieveJSON(url, new JSONObject());
				if(accounts.has("account") && accounts.getJSONArray("account").length() >0){
					if(accounts.getJSONArray("account").getJSONObject(0).length() >0 ){
						//refuse to delete as has roles attached
						UIException uiexception =  new UIException("This Role has Accounts associated with it");
						request.sendJSONResponse(uiexception.getJSON());
						return;
					}
				}
				
			}
			storage.deleteJSON(base+"/"+path);
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented ",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		} catch (JSONException e) {
			throw new UIException("JSONException ",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}
