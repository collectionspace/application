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
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(base,fields);
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
		String roleName = "ROLE_TENANT_ADMINISTRATOR";
		String roleId = "47b727ac-91c9-4df4-b65f-823ae5a88e32";
		JSONObject roleitem = new JSONObject();
		roleitem.put("roleName", roleName);
		roleitem.put("roleId", roleId);
		
		JSONArray role = new JSONArray();
		role.put(roleitem);
		JSONObject rolesobj = new JSONObject();
		rolesobj.put("role",role);
		JSONArray roles = new JSONArray();
		roles.put(rolesobj);
		JSONObject fields = new JSONObject();
		JSONObject datafields = data.getJSONObject("fields");
		JSONObject account = new JSONObject();
		account.put("accountId", path);
		account.put("userId", datafields.getString("userId"));
		account.put("screenName", datafields.getString("screenName"));
		JSONArray accounts = new JSONArray();
		accounts.put(account);
		
		
		JSONObject accountrole = new JSONObject();
		fields.put("account", accounts);
		fields.put("roles", roles);
		accountrole.put("fields", fields);
		
		if(fields!=null)
			path=storage.autocreateJSON(base,fields);
		
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		JSONObject data = null;
		data=request.getJSONBody();
		
		boolean notfailed = true;
		String msg="";
		try{
			try{
				if(create) {
					path=sendJSON(storage,null,data);
					assignRole(storage,path,data);
					//assign to default role.
				} else{
					path=sendJSON(storage,path,data);
				}
				if(path==null){
					throw new UIException("Insufficient data for create (no fields?)");
				}
			} catch (ExistException x) {
				throw new UIException("Existence exception: "+x,x);
			} catch (UnimplementedException x) {
				throw new UIException("Unimplemented exception: "+x,x);
			} catch (UnderlyingStorageException x) {
				throw new UIException("Problem storing: "+x,x);
			} 

			data.put("csid",path);
			data.put("ok",notfailed);
			data.put("message",msg);
			request.sendJSONResponse(data);
			request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			if(create&&notfailed)
				request.setSecondaryRedirectPath(new String[]{url_base,path});
		}	catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		}
		
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	
	public void configure(WebUI ui,Spec spec) {}
}