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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAutoComplete implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebAutoComplete.class);
	private Record r;
	
	public WebAutoComplete(Record r) { this.r=r; }
	
	private String[] doAutocomplete(CSPRequestCache cache,Storage storage,String fieldname,String start) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		FieldSet fs=r.getField(fieldname);
		List<String> out=new ArrayList<String>();
		if(fs == null){//try and find field in repeats if not generall available
			fs=r.getRepeatField(fieldname);
		}
		
		if(!(fs instanceof Field))
			return new String[0]; // Cannot autocomplete on groups
		
		//support multiassign of autocomplete instances
		for(Instance n : ((Field)fs).getAllAutocompleteInstances()) {
			if(n==null){
				// Field has no autocomplete
			}
			else{
				String path=n.getRecord().getID()+"/"+n.getTitleRef();
				JSONObject restriction=new JSONObject();
				restriction.put(n.getRecord().getDisplayNameField().getID(),start); // May be something other than display name
				//XXX how do we do pagination for autocomplete?
				JSONObject results = storage.getPathsJSON(path,restriction);
				String[] paths = (String[]) results.get("listItems");
				for(String csid : paths) {
					JSONObject data=storage.retrieveJSON(path+"/"+csid+"/view");
					JSONObject entry=new JSONObject();
					entry.put("urn",data.get("refid"));
					entry.put("label",data.getString(n.getRecord().getDisplayNameField().getID()));
					out.add(entry.toString());
				}
			}
		}
		
		//Instance n=((Field)fs).getAutocompleteInstance();
		if(out==null){
			return new String[0]; 
		}
		return out.toArray(new String[0]);
	}
	
	private void autocomplete(CSPRequestCache cache,Storage storage,UIRequest request) throws UIException {
		try {
			TTYOutputter tty=request.getTTYOutputter();
			String[] path=request.getPrincipalPath();
			for(String v : doAutocomplete(cache,storage,path[path.length-1],request.getRequestArgument("q"))) {
				tty.line(v);
			}
			tty.flush();
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
		autocomplete(q.getCache(),q.getStorage(),q.getUIRequest());
	}

	public void configure() throws ConfigException {}
	
	public void configure(WebUI ui,Spec spec) {}
}
