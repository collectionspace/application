package org.collectionspace.chain.csp.webui.misc;

import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAutoComplete implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebAutoComplete.class);
	private Record r;
	
	public WebAutoComplete(Record r) { this.r=r; }
	
	private JSONArray doAutocomplete(CSPRequestCache cache,Storage storage,String fieldname,String start, String pageSize, String pageNum) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		FieldSet fs=r.getRepeatField(fieldname);
		JSONArray out = new JSONArray();
		//List<String> out=new ArrayList<String>();
		
		if(!(fs instanceof Field))
			return out; // Cannot autocomplete on groups
		
		//support multiassign of autocomplete instances
		for(Instance n : ((Field)fs).getAllAutocompleteInstances()) {
			if(n==null){
				// Field has no autocomplete
			}
			else{
				String path=n.getRecord().getID()+"/"+n.getTitleRef();
				JSONObject restriction=new JSONObject();
				if(pageSize!=null) {
					restriction.put("pageSize",pageSize);
				}
				if(pageNum!=null) {
					restriction.put("pageNum",pageNum);
				}
				restriction.put(n.getRecord().getDisplayNameField().getID(),start); // May be something other than display name
				
				JSONObject results = storage.getPathsJSON(path,restriction);
				String[] paths = (String[]) results.get("listItems");
				for(String csid : paths) {
					JSONObject data=storage.retrieveJSON(path+"/"+csid+"/view");
					JSONObject entry=new JSONObject();
					entry.put("urn",data.get("refid"));
					entry.put("label",data.getString(n.getRecord().getDisplayNameField().getID()));
					out.put(entry);
				}
			}
		}
		
		//Instance n=((Field)fs).getAutocompleteInstance();
		return out;
	}
	
	private void autocomplete(CSPRequestCache cache,Storage storage,UIRequest request) throws UIException {
		try {

			String[] path=request.getPrincipalPath();
			JSONArray out = doAutocomplete(cache,storage,path[path.length-1],request.getRequestArgument("q"),request.getRequestArgument("pageSize"),request.getRequestArgument("pageNum"));
			
			request.sendJSONResponse(out);
			
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (UnderlyingStorageException x) {
			throw new UIException("UnderlyingStorageException during autocompletion"+x.getLocalizedMessage(),x.getStatus(),x.getUrl(),x);
		}
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		autocomplete(q.getCache(),q.getStorage(),q.getUIRequest());
	}

	public void configure() throws ConfigException {}
	
	public void configure(WebUI ui,Spec spec) {}
}
