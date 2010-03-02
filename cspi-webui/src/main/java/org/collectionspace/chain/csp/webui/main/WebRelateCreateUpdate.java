package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
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

public class WebRelateCreateUpdate implements WebMethod {
	private boolean create;
	private Map<String,String> url_to_type=new HashMap<String,String>();

	public WebRelateCreateUpdate(boolean create) {
		this.create=create;
	}

	private JSONObject createServicesObject(String src_type,String src,String type,String dst_type,String dst) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("src",url_to_type.get(src_type)+"/"+src);
		out.put("dst",url_to_type.get(dst_type)+"/"+dst);
		out.put("type",type);
		return out;
	}
	private JSONObject convertPayload(JSONObject in,boolean reverse) throws JSONException { // XXX check for errors
		JSONObject source=in.getJSONObject("source");
		JSONObject target=in.getJSONObject("target");
		if(reverse) {
			JSONObject tmp=source;
			source=target;
			target=tmp;
		}
		String type=in.getString("type");		
		return createServicesObject(source.getString("recordtype"),source.getString("csid"),type,target.getString("recordtype"),target.getString("csid"));
	}
	
	private String sendJSONOne(Storage storage,String path,JSONObject data,boolean reverse) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=convertPayload(data,reverse);
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON("/relations/main/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON("/relations/main",fields);
		}
		return path;
	}

	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		boolean one_way=false;
		if(data.getBoolean("one-way"))
			one_way=true;
		String out=sendJSONOne(storage,path,data,false);
		if(!one_way)
			sendJSONOne(storage,path,data,true);			
		return out;
	}
	
	private void relate(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=request.getJSONBody();
			if(create) {
				path=sendJSON(storage,null,data);
			} else
				path=sendJSON(storage,path,data);
			if(path==null)
				throw new UIException("Insufficient data for create (no fields?)");
			data.put("csid",path);
			request.sendJSONResponse(data);
			request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			request.setSecondaryRedirectPath(new String[]{"relationships",path}); // XXX should be derivable
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new UIException("Problem storing: "+x,x);
		}

	}

	public void configure(WebUI ui, Spec spec) {
		for(Record r : spec.getAllRecords()) {
			url_to_type.put(r.getWebURL(),r.getID());
		}
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		relate(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
