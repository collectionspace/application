/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VocabulariesCreateUpdate implements WebMethod {
	private boolean create;
	protected String base;
	private Instance n;
	private Record r;

	private  VocabulariesRead reader;

	public VocabulariesCreateUpdate(Instance n,boolean create) {
		this.create=create;
		this.n=n;
		reader=new VocabulariesRead(n, VocabulariesRead.GET_BASIC_INFO);
	}

	public VocabulariesCreateUpdate(Record r,boolean create) {
		this.create=create;
		this.r=r;
		this.base=r.getID();
	}
	
	
	public void configure(WebUI ui, Spec spec) {}
	
	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		String pathstart = n.getRecord().getID()+"/"+n.getTitleRef();
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(pathstart+"/"+path,fields, new JSONObject());
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(pathstart,fields,null);
		}
		
		// XXX no vocabulary relations for now. Naming is too complex.
		return path;
	}
	private String createItem(Storage storage,UIRequest request,String path, JSONObject data) throws UIException, JSONException, ExistException, UnimplementedException, UnderlyingStorageException{


		if(data.has("namespace")){
			if(!n.getWebURL().equals(data.getString("namespace"))){
				throw new UIException("namespace did not match"+data.getString("namespace")+":"+n.getWebURL());
			}
		}
		if(create) {
			path=sendJSON(storage,null,data);
		} else
			path=sendJSON(storage,path,data);
		if(path==null)
			throw new UIException("Insufficient data for create (no fields?)");
		
		return path;
	}
	
	private String createInstance(Storage storage,UIRequest request,String path, JSONObject data) throws UIException, JSONException, ExistException, UnimplementedException, UnderlyingStorageException{
		
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null && !path.equals("")) {
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields, new JSONObject());
		} else {
		// Create
			if(fields!=null)
				path=storage.autocreateJSON(base,fields,null);
		}
		return path;
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=request.getJSONBody();
			String redirectpath = "";

			//is this an instance or an item?
			if(this.r == null && this.n != null){
				FieldSet displayNameFS = n.getRecord().getDisplayNameField();
				String displayNameFieldName = (displayNameFS!=null)?displayNameFS.getID():null;
				boolean quickie = false;
				String quickieDisplayName = null;
				if(create) {
					quickie = 
						(data.has("_view") && data.getString("_view").equals("autocomplete"));
					// Check to see if displayName field needs remapping from UI
					if(quickie && !"displayName".equals(displayNameFieldName)) {
						// Need to map the field for displayName, and put it into a proper structure
						JSONObject fields = data.getJSONObject("fields");
						quickieDisplayName = fields.getString("displayName");
						if(quickieDisplayName != null) {
							// displayNames are nested now, so must have a field parent
							FieldSet parentTermGroup = (FieldSet)displayNameFS.getParent();
							JSONArray parentTermInfoArray = new JSONArray();
							JSONObject termInfo = new JSONObject();
							termInfo.put(displayNameFieldName, quickieDisplayName);
							parentTermInfoArray.put(termInfo);
							fields.put(parentTermGroup.getID(), parentTermInfoArray);
							fields.remove("displayName");
						}
					}
				}
				path = createItem(storage,request,path,data);
				data=reader.getJSON(storage,path);
				String refid = data.getJSONObject("fields").getString("refid");
				data.put("urn", refid);
				data.getJSONObject("fields").put("urn", refid);
				data.put("csid",data.getJSONObject("fields").getString("csid"));
				
				if(quickie){
					JSONObject newdata = new JSONObject();
					newdata.put("urn", refid);
					newdata.put("displayName",quickieDisplayName);
					data = newdata;
				}
				redirectpath = n.getWebURL();
			}
			if(this.r != null && this.n == null){
				path = createInstance(storage,request,path,data);
				redirectpath = data.getJSONObject("fields").getString("shortIdentifier");
			}
			
			request.sendJSONResponse(data);
			request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			if(create)
				request.setSecondaryRedirectPath(new String[]{redirectpath,path});
				//request.setSecondaryRedirectPath(new String[]{n.getWebURL(),path});
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
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
