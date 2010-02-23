package org.collectionspace.chain.csp.webui.main;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebSearchList implements WebMethod {
	private boolean search;
	private String base;
	
	public WebSearchList(String base,boolean search) {
		this.base=base;
		this.search=search;
	}
		
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view");
		out.put("csid",csid);
		out.put("recordtype",WebUI.convertTypeToTypeURL(type));
		return out;		
	}
	
	private JSONObject generateEntry(Storage storage,String base,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		return generateMiniRecord(storage,base,member);
	}
	
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,base,p));
		out.put(key,members);
		return out;
	}
	
	private void search_or_list(Storage storage,UIRequest ui,String param) throws UIException {
		try {
			JSONObject restriction=new JSONObject();
			String key="items";
			if(param!=null) {
				restriction.put("keywords",param);
				key="results";
			}
			String[] paths=storage.getPaths(base,restriction);
			for(int i=0;i<paths.length;i++) {
				if(paths[i].startsWith(base+"/"))
					paths[i]=paths[i].substring((base+"/").length());
			}
			ui.sendJSONResponse(pathsToJSON(storage,base,paths,key));
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("UnderlyingStorageException during autocompletion",e);
		}			
	}
	

	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null);
	}

	public void configure(ReadOnlySection config) throws ConfigException {}
	public void configure_finish() {}
}
