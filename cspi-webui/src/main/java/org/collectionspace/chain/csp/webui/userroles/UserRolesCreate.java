package org.collectionspace.chain.csp.webui.userroles;

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
import org.json.JSONException;
import org.json.JSONObject;

public class UserRolesCreate implements WebMethod{

	private String url_base,base;

	public UserRolesCreate(Record r){
		this.url_base=r.getSubRecord("userrole").getID();
		this.base=r.getID();
	}
	
	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		// Create
		if(fields!=null)
			path=storage.autocreateJSON(base,fields);
		return path;
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		JSONObject data = null;
		data=request.getJSONBody();
		
		boolean notfailed = true;
		String msg="";
		try{
			try{
				path=sendJSON(storage,null,data);
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
			request.setOperationPerformed(Operation.CREATE);
			if(notfailed)
				request.setSecondaryRedirectPath(new String[]{url_base,path});
		}	catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		}
		
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}
	
	public void configure(WebUI ui, Spec spec) {}
	public void configure() throws ConfigException {}

}
