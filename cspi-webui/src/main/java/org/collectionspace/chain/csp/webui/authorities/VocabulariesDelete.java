/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabulariesDelete implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(VocabulariesDelete.class);
	private Instance n;
	
	public VocabulariesDelete(Instance n) {
		this.n=n;
	}

	public void configure(WebUI ui, Spec spec) {}

	private void store_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			String url = n.getRecord().getID()+"/"+n.getTitleRef()+"/"+path;
			JSONObject test = storage.retrieveJSON(url+"/refObjs", new JSONObject());
			if(test.has("items") && (test.getJSONArray("items").length() > 0)){
				UIException uiexception =  new UIException("This Vocabulary Item has Procedures associated with it");
				request.sendJSONResponse(uiexception.getJSON());
				return;
			}
			storage.deleteJSON(url);
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (JSONException e) {
			throw new UIException("JSON Not found (malformed refObjs payload) "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
