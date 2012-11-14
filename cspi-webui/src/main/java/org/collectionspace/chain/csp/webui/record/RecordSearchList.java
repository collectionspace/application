/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.record;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.misc.Generic;
import org.collectionspace.chain.csp.webui.misc.GenericSearch;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordSearchList implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RecordSearchList.class);
	private boolean search;
	private String base;
	private Spec spec;
	private Record r;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public RecordSearchList(Record r,boolean search) {
		this.r = r;
		this.spec=r.getSpec();
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
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws JSONException  {
		String postfix = "list";
		if(this.search){
			postfix = "search";
		}
		JSONObject restrictions = new JSONObject();
		JSONObject out = new JSONObject();
		try {
			if(csid == null || csid.equals("")){
				return out;
			}
			out = storage.retrieveJSON(type+"/"+csid+"/view/"+postfix,restrictions);
			out.put("csid",csid);
			String recordtype = null;
			if(!r.isType("searchall")) {
				recordtype = type_to_url.get(type);
			} else {
				JSONObject summarylist = out.getJSONObject("summarylist");
				String uri = summarylist.getString("uri");
				if(uri!=null && uri.startsWith("/"))
					uri=uri.substring(1);
				
				String[] parts=uri.split("/");
				String recordurl = parts[0];
				Record itemr = r.getSpec().getRecordByServicesUrl(recordurl);
				recordtype = type_to_url.get(itemr.getID());
			}
			out.put("recordtype", recordtype);
			// CSPACE-2894
			if(this.r.getID().equals("permission")){
				String summary = out.getString("summary");
				String name = Generic.ResourceNameUI(this.r.getSpec(), summary);
				if(name.contains(WORKFLOW_SUB_RESOURCE)){
					return null;
				}
				out.put("summary", name);
				out.put("display", Generic.getPermissionView(this.r.getSpec(), summary));
			}
		} catch (ExistException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "Exist Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} catch (UnimplementedException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "Exist Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} catch (UnderlyingStorageException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "Exist Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} 
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
		for(String p : paths){
			JSONObject temp = generateEntry(storage,base,p);
			if(temp !=null){
				members.put(temp);
			}
		}
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
	private void search_or_list(Storage storage,UIRequest ui,String path) throws UIException {
		try {
			JSONObject restrictedkey = GenericSearch.setRestricted(ui,null,null,null,search,this.r);
			JSONObject restriction = restrictedkey.getJSONObject("restriction");
			String key = restrictedkey.getString("key");
			
			JSONObject results = new JSONObject();

			if(this.r.getID().equals("permission")){
				//pagination isn't properly implemented in permissions so just keep looping til we get everything
				int pgnum = 0;
				if(restriction.has("pageNum")){ //just get teh page specified
					results = getJSON(storage,restriction,key,base);
				}
				else{ // if not specified page then loop over them all.
					JSONArray newitems =new JSONArray();
					results = getJSON(storage,restriction,key,base);
					while(results.has(key) && results.getJSONArray(key).length()>0){

						JSONArray items = results.getJSONArray(key);
						for(int i=0;i<items.length();i++){
							newitems.put(items.get(i));
						}
						pgnum++;
						restriction.put("pageNum", Integer.toString(pgnum));
						
						results = getJSON(storage,restriction,key,base);
					}
					results.put(key, newitems);
				}
			}
			else if(r.getID().equals("reports")){
				String type= "";
				if(path!=null && !path.equals("")){
					restriction.put("queryTerm", "doctype");
					restriction.put("queryString", spec.getRecordByWebUrl(path).getServicesTenantSg());
				}
				
				if(restriction.has("queryTerm") && restriction.getString("queryTerm").equals("doctype")){
					type = restriction.getString("queryString");
					results = getJSON(storage,restriction,key,base);
					results = showReports(results, type, key);
				}
				else{
					JSONObject reporting = new JSONObject();
					for(Record r2 : spec.getAllRecords()) {
						if(r2.isInRecordList()){
							type = r2.getServicesTenantSg();
							restriction.put("queryTerm","doctype");
							restriction.put("queryString",type);
						
							JSONObject rdata = getJSON(storage,restriction,key,base);
							JSONObject procedurereports = showReports(rdata, type, key);
							reporting.put(r2.getWebURL(), procedurereports);
						}
					}
					results.put("reporting", reporting);
				}
			}				
			else if(r.getID().equals("batch")){
				String type= "";
				if(path!=null && !path.equals("")){
					restriction.put("queryTerm", "doctype");
					restriction.put("queryString", spec.getRecordByWebUrl(path).getServicesTenantSg());
				}
				
				if(restriction.has("queryTerm") && restriction.getString("queryTerm").equals("doctype")){
					type = restriction.getString("queryString");
					results = getJSON(storage,restriction,key,base);
					results = showBatches(results, type, key);
				}
				else{
					JSONObject batch = new JSONObject();
					for(Record r2 : spec.getAllRecords()) {
						if(r2.isInRecordList()){
							type = r2.getServicesTenantSg();
							restriction.put("queryTerm","doctype");
							restriction.put("queryString",type);
						
							JSONObject rdata = getJSON(storage,restriction,key,base);
							JSONObject procedurebatches = showBatches(rdata, type, key);
							batch.put(r2.getWebURL(), procedurebatches);
						}
					}
					results.put("batch", batch);
				}
			}
			else{
				results = getJSON(storage,restriction,key,base);
			}
			//cache for record traverser
			if(results.has("pagination") && results.getJSONObject("pagination").has("separatelists")){
				GenericSearch.createTraverser(ui, this.r.getID(), "", results, restriction, key, 1);
			}
			ui.sendJSONResponse(results);
		} catch (JSONException e) {
			throw new UIException("JSONException during search_or_list",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during search_or_list",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during search_or_list",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			ui.sendJSONResponse(uiexception.getJSON());
		}			
	}
	

	private JSONObject showReports(JSONObject data, String type, String key) throws JSONException{
		JSONObject results = new JSONObject();
		JSONArray list = new JSONArray();
		JSONArray names = new JSONArray();
		
		if(data.has(key)){
			JSONArray ja = data.getJSONArray(key);
	
			for(int j=0;j<ja.length();j++){
				list.put(ja.getJSONObject(j).getString("csid"));
				names.put(ja.getJSONObject(j).getString("number"));
			}
			results.put("reportlist", list);
			results.put("reportnames", names);
		}
		return results;
	}				

	private JSONObject showBatches(JSONObject data, String type, String key) throws JSONException{
		JSONObject results = new JSONObject();
		JSONArray list = new JSONArray();
		JSONArray names = new JSONArray();
		JSONArray newFocuses = new JSONArray();
		
		if(data.has(key)){
			JSONArray ja = data.getJSONArray(key);
	
			for(int j=0;j<ja.length();j++){
				list.put(ja.getJSONObject(j).getString("csid"));
				names.put(ja.getJSONObject(j).getString("number"));
				
				JSONObject summarylist = ja.getJSONObject(j).getJSONObject("summarylist");
				newFocuses.put(summarylist.getBoolean("createsNewFocus"));
			}
			results.put("batchlist", list);
			results.put("batchnames", names);
			results.put("batchnewfocuses", newFocuses);
		}
		return results;
	}
	
	private void advancedSearch(Storage storage,UIRequest ui,String path, JSONObject params) throws UIException{

		try {

			JSONObject results = new JSONObject();
			JSONObject restrictedkey = GenericSearch.setRestricted(ui,null,null,null,search,this.r);
			JSONObject restriction = restrictedkey.getJSONObject("restriction");
			String key = restrictedkey.getString("key");
			GenericSearch.buildQuery(this.r,params, restriction);
			
			key="results";

			results = getJSON(storage,restriction,key,base);

			//cache for record traverser
			if(results.has("pagination") && results.getJSONObject("pagination").has("separatelists")){
				GenericSearch.createTraverser(ui, this.r.getID(), "", results, restriction, key, 1);
			}
			ui.sendJSONResponse(results);
		} catch (JSONException e) {
			throw new UIException("JSONException during advancedSearch "+e.getMessage(),e);
		} catch (ExistException e) {
			throw new UIException("ExistException during search_or_list",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during search_or_list",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			ui.sendJSONResponse(uiexception.getJSON());
		}	
	}



	public void searchtype(Storage storage,UIRequest ui,String path) throws UIException{

		if(ui.getBody() == null || StringUtils.isBlank(ui.getBody())){
			search_or_list(storage,ui,path);
		}
		else{
			//advanced search
			advancedSearch(storage,ui,path, ui.getJSONBody());
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
			if(paths[i].startsWith(mybase+"/")){
				paths[i]=paths[i].substring((mybase+"/").length());
			}
		}
		out = pathsToJSON(storage,mybase,paths,key,pagination);
		return out;
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		searchtype(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
