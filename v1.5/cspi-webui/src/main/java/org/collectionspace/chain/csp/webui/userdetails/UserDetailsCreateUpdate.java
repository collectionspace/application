/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.userdetails;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
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

public class UserDetailsCreateUpdate implements WebMethod {
	private String url_base,base;
	private boolean create;
	private Spec spec;
	
	public UserDetailsCreateUpdate(Record r,boolean create) { 
		spec=r.getSpec();
		this.url_base=r.getWebURL();
		this.base=r.getID();
		this.create=create;
	}
	
	
	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null) {
			// Update
			if(fields!=null){
				storage.updateJSON(base+"/"+path,fields);
			}
		} else {
			// Create
			if(fields!=null){
				path=storage.autocreateJSON(base,fields);
			}
		}

		return path;
	}
			
	/**
	 * This is here until we properly implement roles. This will automatically assign a role to a user on creation
	 * @param storage
	 * @param path
	 * @param data
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	private void assignRole(Storage storage, String path, JSONObject data) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{
		JSONObject fields=data.optJSONObject("fields");
		
		JSONArray roledata = new JSONArray();
		
		if(fields.has("role")){
			JSONArray roles = fields.getJSONArray("role");
			for(int i=0;i<roles.length();i++){
				JSONObject role = roles.getJSONObject(i);
				if(role.getString("roleSelected").equals("true")){
					JSONObject roleitem = new JSONObject();
					roleitem.put("roleName", role.getString("roleName"));
					roleitem.put("roleId", role.getString("roleId"));
					roledata.put(roleitem);
				}
			}
		}
		else{
			//temporary munge so new users can login

			String roleName = "ROLE_TENANT_ADMINISTRATOR";
			//find csid for roleName

			JSONObject restriction=new JSONObject();
			restriction.put("keywords",roleName);
			JSONObject testroledata = storage.getPathsJSON("/role",restriction);
			String[] paths = (String[]) testroledata.get("listItems");
			String roleId = "";
			
			for(int i=0;i<paths.length;i++) {
				JSONObject out=storage.retrieveJSON("/role/"+paths[i]+"", new JSONObject());
				String test = out.toString();
				if(out.getString("roleName").equals(roleName)){
					roleId=paths[i];
				}
			}
			if(!roleId.equals("")){

				JSONObject roleitem = new JSONObject();
				roleitem.put("roleName", roleName);
				roleitem.put("roleId", roleId);
				
				roledata.put(roleitem);
			}
		}
		

		JSONObject account = new JSONObject();
		account.put("accountId", path);
		account.put("userId", fields.getString("userId"));
		account.put("screenName", fields.getString("screenName"));
		

		JSONObject accountrole = new JSONObject();
		JSONObject arfields = new JSONObject();
		arfields.put("account", account);
		arfields.put("role", roledata);
		accountrole.put("fields", arfields);
		
		if(fields!=null)
			path=storage.autocreateJSON(spec.getRecordByWebUrl("userrole").getID(),arfields);
	
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		JSONObject data = null;
		data=request.getJSONBody();
		
		boolean notfailed = true;
		String msg="";
		try {
			if (create) {
				path = sendJSON(storage, null, data);
				// assign to default role.
			} else {
				path = sendJSON(storage, path, data);
			}
			assignRole(storage, path, data);
			if (path == null) {
				throw new UIException(
						"Insufficient data for create (no fields?)");
			}
			data.put("csid", path);
			boolean isError = !notfailed;
			data.put("isError", isError);
			JSONObject messages = new JSONObject();
			messages.put("message", msg);
			messages.put("severity", "info");
			JSONArray arr = new JSONArray();
			arr.put(messages);
			data.put("messages", arr);
			request.sendJSONResponse(data);
			request.setOperationPerformed(create ? Operation.CREATE
					: Operation.UPDATE);
			if (create && notfailed)
				request.setSecondaryRedirectPath(new String[] { url_base, path });
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: ", x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: ", x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: ", x);
		} catch (UnderlyingStorageException x) {
			UIException uiexception = new UIException(x.getMessage(), x
					.getStatus(), x.getUrl(), x);
			request.sendJSONResponse(uiexception.getJSON());
		}

		
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	
	public void configure(WebUI ui,Spec spec) {}
}