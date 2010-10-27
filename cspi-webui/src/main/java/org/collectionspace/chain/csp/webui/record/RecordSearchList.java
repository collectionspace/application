/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.record;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

public class RecordSearchList implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RecordSearchList.class);
	private boolean search;
	private String base;
	private Record r;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public RecordSearchList(Record r,boolean search) {
		this.r = r;
		this.base=r.getID();
		this.search=search;
	}
	
	/**
	 * Retrieve the mini summary information e.g. summary and number and append the csid and recordType to it
	 * @param {Storage} storage Type of storage (e.g. AuthorizationStorage, RecordStorage,...)
	 * @param {String} type The type of record requested (e.g. permission)
	 * @param {String} csid The csid of the record
	 * @return {JSONObject} The JSON string containing the mini record
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		String postfix = "list";
		if(this.search){
			postfix = "search";
		}
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view/"+postfix);
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;
	}
	
	/**
	 * Intermediate function to generateMiniRecord. This function only exists for if someone would like to create different types
	 * of records e.g. MiniRecordA, MiniRecordB,...
	 * @param {Storage} storage The type of storage (e.g. AuthorizationStorage,RecordStorage,...) 
	 * @param {String} base The type of record (e.g. permission)
	 * @param {String} member The csid of the object
	 * @return {JSONObject} A JSONObject containing the mini record.
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	private JSONObject generateEntry(Storage storage,String base,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		return generateMiniRecord(storage,base,member);
	}
	
	/**
	 * Creates a list of results containing:summary, number, recordType, csid
	 * @param {Storage} storage The type of storage (e.g. AuthorizationStorage,RecordStorage,...)
	 * @param {String} base The type of record (e.g. permission)
	 * @param {String[]} paths The list of csids from the records that were requested
	 * @param {String} key The surrounding key for the results (e.g. {"key":{...}})
	 * @return {JSONObject} The JSONObject that is sent back to the UI Layer
	 * @throws JSONException
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key, JSONObject pagination) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,base,p));
		out.put(key,members);
		
		if(pagination!=null){
			out.put("pagination",pagination);
		}
		return out;
	}
	
	
	
	/**
	 * This function is the general function that calls the correct funtions to get all the data that the UI requested and get it in the 
	 * correct format for the UI.
	 * @param {Storage} storage The type of storage requested (e.g. RecordStorage, AuthorizationStorage,...) 
	 * @param {UIRequest} ui The request from the ui to which we send a response.
	 * @param {String} param If a querystring has been added to the URL(e.g.'?query='), it will be in this param 
	 * @param {String} pageSize The amount of results per page requested.
	 * @param {String} pageNum The amount of pages requested.
	 * @throws UIException
	 */
	private void search_or_list(Storage storage,UIRequest ui) throws UIException {
		try {
			JSONObject restriction=new JSONObject();
			String key="items";
			
			Set<String> args = ui.getAllRequestArgument();
			for(String restrict : args){
				if(ui.getRequestArgument(restrict)!=null){
					String value = ui.getRequestArgument(restrict);
					if(restrict.equals("query") && search){
						restrict = "keywords";
						key="results";
					}
					if(restrict.equals("pageSize")||restrict.equals("pageNum")||restrict.equals("keywords")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("query")){
						//ignore - someone was doing something odd
					}
					else{
						//XXX I would so prefer not to restrict and just pass stuff up but I know it will cause issues later
						restriction.put("queryTerm",restrict);
						restriction.put("queryString",value);
					}
				}
			}
			
			JSONObject returndata = getJSON(storage,restriction,key,base);
			ui.sendJSONResponse(returndata);
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
	

	/* Wrapper exists to be used inRead, hence not private */
	public JSONObject getJSON(Storage storage,JSONObject restriction, String key, String mybase) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException{
		JSONObject out = new JSONObject();

		JSONObject data = storage.getPathsJSON(mybase,restriction);
		String[] paths = (String[]) data.get("listItems");
		JSONObject pagination = new JSONObject();
		if(data.has("pagination")){
			pagination = data.getJSONObject("pagination");
		}
		
		for(int i=0;i<paths.length;i++) {
			if(paths[i].startsWith(mybase+"/"))
				paths[i]=paths[i].substring((mybase+"/").length());
		}
		out = pathsToJSON(storage,mybase,paths,key,pagination);
		return out;
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		search_or_list(q.getStorage(),q.getUIRequest());
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
