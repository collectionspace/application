/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.userdetails;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.httpclient.HttpStatus;
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
import org.collectionspace.csp.api.ui.UISession;
import org.collectionspace.csp.helper.core.ResponseCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDetailsCreateUpdate implements WebMethod {
	private static final Logger log = LoggerFactory.getLogger(UserDetailsCreateUpdate.class);
	
	private String url_base,base;
	private boolean create;
	private Spec spec;
	
	private static final String PASSWORD_FIELD = "password";	// Should be elsewhere
	private static final String USER_ID_FIELD = "userId";	// Should be elsewhere
	
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
				storage.updateJSON(base+"/"+path,fields, new JSONObject());
			}
		} else {
			// Create
			if(fields!=null){
				path=storage.autocreateJSON(base,fields,null);
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
		// If we are updating a role, then we need to clear the userperms cache
		// Note that creating a role does not impact things until we assign it
		if(!create) {
			ResponseCache.clearCache(ResponseCache.USER_PERMS_CACHE);
		}

		
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

			// TODO New users should probably get the READER role, not admin
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
		account.put(USER_ID_FIELD, fields.getString(USER_ID_FIELD));
		account.put("screenName", fields.getString("screenName"));
		

		JSONObject accountrole = new JSONObject();
		JSONObject arfields = new JSONObject();
		arfields.put("account", account);
		arfields.put("role", roledata);
		accountrole.put("fields", arfields);
		
		if(fields!=null)
			path=storage.autocreateJSON(spec.getRecordByWebUrl("userrole").getID(),arfields,null);
	
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		JSONObject data = null;
		data=request.getJSONBody();
		
		boolean notfailed = true;
		String msg="";
		try {
			boolean currentUserPasswordChange = false;
			String newPassword = null;
			boolean absorbedSvcsError = false;
			if (create) {
				path = sendJSON(storage, null, data);
				// assign to default role.
			} else {
				// Check for password update. If doing that, absorb 403 errors and redirect
				// as though we are doing a logout.
				JSONObject fields=data.optJSONObject("fields");
				if(fields!=null && fields.has(PASSWORD_FIELD)) {
					String passwd = fields.getString(PASSWORD_FIELD);
					if(passwd!=null) {
						if(passwd.isEmpty()) {
							fields.remove(PASSWORD_FIELD); // Preclude removl of a password
						} else {
							String editedUserId = fields.getString(USER_ID_FIELD);
							UISession session = request.getSession();
							if(session != null) {
								Object currentUserId = session.getValue(UISession.USERID);
								if(currentUserId!= null && currentUserId.equals(editedUserId)) {
									newPassword = passwd;
									currentUserPasswordChange = true;
								}
							}
						}
					}
				}
				path = sendJSON(storage, path, data);
				// If that succeeded, and if we updated the current password, set session
				// credentials
				if(currentUserPasswordChange) {
					request.getSession().setValue(UISession.PASSWORD,newPassword);
				}
			}
			if (path == null) {
				throw new UIException(
						"Insufficient data for create (no fields?)");
			}
			data.put("csid", path);
			try {
				assignRole(storage, path, data);
			} catch(UnderlyingStorageException usex) {
				Integer status = usex.getStatus();
				if(status != null
					&& (status == HttpStatus.SC_FORBIDDEN
						|| status == HttpStatus.SC_UNAUTHORIZED)) {
					absorbedSvcsError = true;
					msg = "Cannot update roles for this account.";
					log.warn("UserDetailsCreateUpdate changing roles, and absorbing error returned: "+usex.getStatus());
				} else {
					throw usex;	// Propagate
				}
			}
			boolean isError = !notfailed;
			data.put("isError", isError);
			JSONObject messages = new JSONObject();
			messages.put("message", msg);
			messages.put("severity", "info");
			JSONArray arr = new JSONArray();
			arr.put(messages);
			data.put("messages", arr);
                        // Elide the value of the password field before returning a response
                        data.optJSONObject("fields").remove(PASSWORD_FIELD);
			request.sendJSONResponse(data);
			request.setOperationPerformed(create ? Operation.CREATE : Operation.UPDATE);
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