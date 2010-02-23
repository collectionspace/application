package org.collectionspace.chain.csp.webui.main;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.impl.main.ConfigException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebRead implements WebMethod {
	private String base;
	private static final Set<String> record_type=new HashSet<String>();
	
	static {
		record_type.add("collection-object");
		record_type.add("intake");
		record_type.add("acquisition");		// XXX hack
	}
	
	public WebRead(String base) { this.base=base; }
		
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view");
		out.put("csid",csid);
		out.put("recordtype",WebUI.convertTypeToTypeURL(type));
		return out;		
	}

	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject in=storage.retrieveJSON("relations/main/"+csid);
		String[] dstid=in.getString("dst").split("/");
		String type=in.getString("type");
		JSONObject mini=generateMiniRecord(storage,dstid[0],dstid[1]);
		mini.put("relationshiptype",type);
		mini.put("relid",in.getString("csid"));
		return mini;
	}
	
	private JSONArray createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONArray out=new JSONArray();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		String[] relations=storage.getPaths("relations/main",restrictions);
		for(String r : relations)
			out.put(generateRelationEntry(storage,r));
		return out;
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			if(record_type.contains(base)) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				JSONArray relations=createRelations(storage,csid);
				out.put("fields",fields);
				out.put("relations",relations);
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

	public void configure(ReadOnlySection config) throws ConfigException {}
	public void configure_finish() {}
}
