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
import org.mortbay.log.Log;


public class UserRolesRead implements WebMethod{
	private String base,base_url,sub_url;
	private boolean record_type;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public UserRolesRead(Record r){
		this.base = r.getID();
		this.base_url = r.getServicesURL();
		this.sub_url = r.getSpec().getRecord("userrole").getServicesURL();
		record_type=r.isType("userdata");
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			if(record_type) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
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
		Log.info(path);
		Log.info(path);
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
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
