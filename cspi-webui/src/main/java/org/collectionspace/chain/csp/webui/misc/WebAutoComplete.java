/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.util.json.JSONUtils;
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
		FieldSet fs=r.getFieldFullList(fieldname);
		JSONArray out = new JSONArray();
		Instance[] allInstances = null;
		if(fs == null || !(fs instanceof Field)){
			if(r.hasHierarchyUsed("screen")){
				Structure s = r.getStructure("screen");
				if(s.hasOption(fieldname)){
					Option a = s.getOption(fieldname);
					String[] data = a.getName().split(",");

					Map<String, Instance> tempinstances = new HashMap<String, Instance>();
					for(String ins : data){
						tempinstances.put(ins, r.getSpec().getInstance(ins));
						allInstances = tempinstances.values().toArray(new Instance[0]);
					}
					
				}
				else{
					fs = r.getSpec().getRecord("hierarchy").getFieldFullList(fieldname);
					if(fs instanceof Field){ 	
						allInstances = ((Field)fs).getAllAutocompleteInstances();
					}
				}
			}
		}
		else{
			allInstances = ((Field)fs).getAllAutocompleteInstances();
		}
		if(allInstances == null){
			return out; // Cannot autocomplete
		}
		
		//support multiassign of autocomplete instances
		for(Instance n : allInstances) {
			try{
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
						JSONObject data=storage.retrieveJSON(path+"/"+csid+"/view", new JSONObject());
						JSONObject entry=new JSONObject();
						// TODO - handle multiple name matches
						String displayNameString = data.getString(n.getRecord().getDisplayNameField().getID());
						JSONArray displayNames = JSONUtils.createJSONArrayFromSeparatedString(displayNameString);
						String primaryDN = displayNames.getString(0);
						String refid = data.getString("refid");
						// HACK - transition period with full instead of base URN value
						if(refid.endsWith("'"+primaryDN+"'"))
							refid = refid.substring(0,refid.length()-(primaryDN.length()+2));
						entry.put("baseUrn",refid);
						entry.put("csid",data.getString("csid"));
						entry.put("type",n.getRecord().getWebURL());
						entry.put("displayNames", displayNames);
						//RefName.AuthorityItem item = RefName.AuthorityItem.parse(refid); 
						//entry.put("namespace",item.getParentShortIdentifier());
						entry.put("namespace",data.getString("namespace"));
						out.put(entry);
					}
				}
			}
			catch(UnderlyingStorageException x){
				if(x.getStatus() == 403){ 
					//permission error - keep calm and carry on
				}
				else{
					throw x;
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
