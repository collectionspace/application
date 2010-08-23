package org.collectionspace.chain.csp.webui.userdetails;

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

public class UserDetailsRead  implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(UserDetailsRead.class);
	private String base;
	private boolean record_type;
	private Record r;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public UserDetailsRead(Record r) { 
		this.base=r.getID();
		this.r = r;
		record_type=r.isType("userdata");
	}
		
	/**
	 * create role array
	 * [{roleId:"","roleName":"",selected:"yes|no"},{ ... }]
	 * @param activeRoles
	 * @return
	 * @throws UnderlyingStorageException 
	 * @throws UnimplementedException 
	 * @throws ExistException 
	 * @throws JSONException 
	 */
	private JSONArray getRoles(Storage storage,JSONObject activeRoles) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException{
		JSONObject set = new JSONObject();
		//get all roles - actually dont
		
		//String filePath = r.getSpec().getRecordByWebUrl("role").getID()+"/";
		//JSONObject roles = storage.retrieveJSON(filePath);
		//log.info("DEBUG"+filePath+roles.toString());
		//mark active roles
		JSONArray roles = activeRoles.getJSONArray("role");
		
		//we are ignoring pagination so this will return the first 40 roles only
		//UI doesn't know what it wants to do about pagination etc
		return roles;
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			if(record_type) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				JSONObject roles = storage.retrieveJSON(base+"/"+csid+"/"+"userrole");
				JSONArray allroles = getRoles(storage,roles);
				fields.put("role",allroles);
				
				out.put("fields",fields);
				out.put("ok",true);
				out.put("message","");
				out.put("relations",new JSONArray());
			} else {
				out=storage.retrieveJSON(base+"/"+csid);
			}
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem getting",e);
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
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}