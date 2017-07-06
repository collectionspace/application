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
	
	private JSONArray doAuthorityAutocomplete(CSPRequestCache cache,Storage storage,String fieldname,
			String start, String vocabConstraint, String pageSize, String pageNum) 
			throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, ConfigException {
		FieldSet fs=r.getFieldFullList(fieldname);
		JSONArray out = new JSONArray();
		Instance[] allInstances = null;
		if(fs == null || !(fs instanceof Field)){
			if(r.hasHierarchyUsed("screen")){
				Structure s = r.getStructure("screen");	// Configures the hierarchy section.
				if(s.hasOption(fieldname)){				// This is one of the hierarchy fields
					if(vocabConstraint!=null) {
						allInstances = new Instance[1];
						String fullname = r.getID()+"-"+vocabConstraint;
						allInstances[0] = r.getSpec().getInstance(fullname);
					} else {
						Option a = s.getOption(fieldname);
						String[] data = a.getName().split(",");
						allInstances = new Instance[data.length];
						for(int i=0; i<data.length; i++){
							allInstances[i] = (r.getSpec().getInstance(data[i]));
						}
					}
				} else {
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
					FieldSet dispNameFS = n.getRecord().getDisplayNameField(); 
					if(dispNameFS==null) {
						throw new ConfigException("WebAutoComplete for Instance has no displayName configured: "+n.getID());
					}
					String displayNameFieldID = dispNameFS.getID(); 
					restriction.put(displayNameFieldID,start); // May be something other than display name
					
					JSONObject results = storage.getPathsJSON(path,restriction);
					String[] paths = (String[]) results.get("listItems");
					for(String csid : paths) {
						JSONObject data=storage.retrieveJSON(path+"/"+csid+"/view", new JSONObject());
						JSONObject entry=new JSONObject();
						// TODO - handle multiple name matches
						String displayNameString = data.getString(displayNameFieldID);
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
						entry.put("workflow", data.getString("workflow"));
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
	
	/**
	 * Handles autocomplete for non-authority records. Simpler model that does not involve
	 * multiple namespaces (a.k.a., vocabs, a.k.a., Instances).
	 * @param cache
	 * @param storage
	 * @param fieldname Should generally only be one of broader* or narrower*
	 * @param start The partial string to match
	 * @param pageSize
	 * @param pageNum
	 * @return the JSON output payload of results.
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws ConfigException 
	 */
	private JSONArray doRecordAutocomplete(CSPRequestCache cache,Storage storage,String fieldname,
			String start, String pageSize, String pageNum) 
			throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, ConfigException {

		// FieldSet fs=r.getFieldFullList(fieldname); we do not really care, frankly
		
		JSONArray out = new JSONArray();
		
		JSONObject restriction=new JSONObject();
		if(pageSize!=null) {
			restriction.put("pageSize",pageSize);
		}
		if(pageNum!=null) {
			restriction.put("pageNum",pageNum);
		}
		
		FieldSet dispNameFS = r.getDisplayNameField(); 
		if(dispNameFS==null) {
			throw new ConfigException("WebAutoComplete for record has no displayName configured: "+r.getID());
		}
		String displayNameFieldID = dispNameFS.getID(); 
		restriction.put(displayNameFieldID,start); // May be something other than display name

		String path = r.getID();
		JSONObject results = storage.getPathsJSON(path,restriction);
		String[] paths = (String[]) results.get("listItems");
		for(String csid : paths) {
			// Appending the "list" to the path will glean the "list" marked fields, into a "summarylist"
			JSONObject data=storage.retrieveJSON(path+"/"+csid+"/view/list", new JSONObject());
			JSONObject entry=new JSONObject();
			JSONObject summarylist=data.getJSONObject("summarylist");
			String displayNameString = summarylist.getString(displayNameFieldID);
			String refName = summarylist.getString("refName");
			// Build a base refName without trailing displayName suffix
			if(refName.endsWith("'"+displayNameString+"'"))
				refName = refName.substring(0,refName.length()-(displayNameString.length()+2));
			entry.put("baseUrn",refName);
			entry.put("csid",summarylist.getString("csid"));
			entry.put("type",r.getWebURL());
			// Standard temCompletion widget handles arrays of alternate terms, so 
			// we pass an array for consistency.
			JSONArray displayNames = new JSONArray();
			displayNames.put(displayNameString);
			entry.put("displayNames", displayNames);
			out.put(entry);
		}
		
		return out;
	}
	
	private void autocomplete(CSPRequestCache cache,Storage storage,UIRequest request) throws UIException {
		try {

			String[] path=request.getPrincipalPath();
			String fieldName = path[path.length-1];	// Last path element is the field on which user is querying.
			JSONArray out = new JSONArray();
			boolean hasHierarchy = r.hasHierarchyUsed("screen");
			boolean isHierarchyAutoComplete = false;
			if(hasHierarchy){
				Structure s = r.getStructure("screen");	// Configures the hierarchy section.
				if(s.hasOption(fieldName)){				// This is one of the hierarchy fields
					isHierarchyAutoComplete = true;
				}
			}
			if(r.isType("authority") || !isHierarchyAutoComplete){
				out = doAuthorityAutocomplete(cache, storage, fieldName,
					request.getRequestArgument(AUTO_COMPLETE_QUERY_PARAM), 
					request.getRequestArgument(CONSTRAIN_VOCAB_PARAM),
					request.getRequestArgument(PAGE_SIZE_PARAM),
					request.getRequestArgument(PAGE_NUM_PARAM));
			} else if(isHierarchyAutoComplete) {
				out = doRecordAutocomplete(cache, storage, fieldName,
						request.getRequestArgument(AUTO_COMPLETE_QUERY_PARAM), 
						request.getRequestArgument(PAGE_SIZE_PARAM),
						request.getRequestArgument(PAGE_NUM_PARAM));
			} else {
				throw new ConfigException("WebAutoComplete called for record that does not support autocomplete!: "+r.getID());
			}
			request.sendJSONResponse(out);
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (ConfigException e) {
			throw new UIException("ConfigException during autocompletion",e);
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
