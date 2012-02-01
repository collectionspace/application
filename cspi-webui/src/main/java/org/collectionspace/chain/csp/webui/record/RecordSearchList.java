/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.record;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
			out.put("recordtype",type_to_url.get(type));
			// CSPACE-2894
			if(this.r.getID().equals("permission")){
				String summary = out.getString("summary");
				String name = Generic.ResourceNameUI(this.r.getSpec(), summary);
				if(name.endsWith("/*/workflow/")){
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
	
	private JSONObject setRestricted(UIRequest ui) throws UIException, JSONException{

		JSONObject returndata = new JSONObject();

		JSONObject restriction=new JSONObject();
		String key="items";
		
		Set<String> args = ui.getAllRequestArgument();
		for(String restrict : args){
			if(!restrict.equals("_")){
				if(ui.getRequestArgument(restrict)!=null){
					String value = ui.getRequestArgument(restrict);
					if(restrict.equals("query") && search){
						restrict = "keywords";
						key="results";
					}
					if(restrict.equals("pageSize")||restrict.equals("pageNum")||restrict.equals("keywords")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortDir")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortKey")){////"summarylist.updatedAt"//movements_common:locationDate
						String[] bits = value.split("\\.");
						String fieldname = value;
						if(bits.length>1){
							fieldname = bits[1];
						}
						FieldSet fs = null;
						if(fieldname.equals("number")){
							fs = r.getMiniNumber();
						}
						else if(fieldname.equals("summary")){
							fs = r.getMiniSummary();
						}
						else{
							//convert sortKey
							fs = r.getFieldFullList(fieldname);
						}

						fieldname = fs.getID();
						FieldSet tmp = fs;
						while(!(tmp.getParent() instanceof Record)){
							tmp = (FieldSet)tmp.getParent();
							if(!tmp.getSearchType().equals("repeator")){
								fieldname = tmp.getServicesParent()[0] +"/0/"+fieldname;
							}
						}
						
						String tablebase = r.getServicesRecordPath(fs.getSection()).split(":",2)[0];
						String newvalue = tablebase+":"+fieldname;
						restriction.put(restrict,newvalue);
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
		}
		returndata.put("key", key);
		returndata.put("restriction", restriction);
		return returndata;
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
			JSONObject restrictedkey = setRestricted(ui);
			JSONObject restriction = restrictedkey.getJSONObject("restriction");
			String key = restrictedkey.getString("key");
			
			JSONObject returndata = new JSONObject();

			if(this.r.getID().equals("permission")){
				//pagination isn't properly implemented in permissions so just keep looping til we get everything
				int pgnum = 0;
				if(restriction.has("pageNum")){ //just get teh page specified
					returndata = getJSON(storage,restriction,key,base);
				}
				else{ // if not specified page then loop over them all.
					JSONArray newitems =new JSONArray();
					returndata = getJSON(storage,restriction,key,base);
					while(returndata.has(key) && returndata.getJSONArray(key).length()>0){

						JSONArray items = returndata.getJSONArray(key);
						for(int i=0;i<items.length();i++){
							newitems.put(items.get(i));
						}
						pgnum++;
						restriction.put("pageNum", Integer.toString(pgnum));
						
						returndata = getJSON(storage,restriction,key,base);
					}
					returndata.put(key, newitems);
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
					returndata = getJSON(storage,restriction,key,base);
					returndata = showReports(returndata, type, key);
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
					returndata.put("reporting", reporting);
				}
			}				
			else{
				returndata = getJSON(storage,restriction,key,base);
			}
			
			ui.sendJSONResponse(returndata);
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
	private void advancedSearch(Storage storage,UIRequest ui,String path, JSONObject params) throws UIException{
		
		try {

			Map<String, String> dates = new HashMap<String, String>();
			JSONObject returndata = new JSONObject();
			JSONObject restrictedkey = setRestricted(ui);
			JSONObject restriction = restrictedkey.getJSONObject("restriction");
			String key = restrictedkey.getString("key");

			String operation = params.getString("operation").toUpperCase();
			JSONObject fields = params.getJSONObject("fields");
			log.debug("Advanced Search on fields: "+fields.toString());

			String asq = ""; 
			Iterator rit=fields.keys();
			while(rit.hasNext()) {
				String join = " ILIKE "; //using ilike so we can have case insensitive searches
				String fieldname=(String)rit.next();
				Object item = fields.get(fieldname);

				String value = "";
				
				if(item instanceof JSONArray){ // this is a repeatable
					JSONArray itemarray = (JSONArray)item;
					for(int j=0;j<itemarray.length();j++){
						JSONObject jo = itemarray.getJSONObject(j);
						Iterator jit=jo.keys();
						while(jit.hasNext()){
							String jname=(String)jit.next();
							if(!jname.equals("_primary")){
								if(jo.get(jname) instanceof String || jo.get(jname) instanceof Boolean ){
									value = jo.getString(jname);
									asq += getAdvancedSearch(jname,value,operation,join);
								}
							}
						}
					}
					
				}
				else if(item instanceof JSONObject){ // no idea what this is
					
				}
				else if(item instanceof String){
					value = (String)item;
					if(!value.equals("")){
						String fieldid = fieldname;
						if(this.r.hasSearchField(fieldname) && this.r.getSearchFieldFullList(fieldname).getUIType().equals("date")){
							if(fieldname.endsWith("Start")){
								fieldid = fieldname.substring(0, (fieldname.length() - 5));
								join = ">= DATE ";
							}
							else if(fieldname.endsWith("End")){
								fieldid = fieldname.substring(0, (fieldname.length() - 3));
								join = "<= DATE ";
							}

							if(dates.containsKey(fieldid)){
								String temp = getAdvancedSearch(fieldid,value,"AND",join);
								String get = dates.get(fieldid);
								dates.put(fieldid, temp + get);
							}
							else{
								String temp = getAdvancedSearch(fieldid,value,"",join);
								dates.put(fieldid, temp);
							}
						}
						else{
							asq += getAdvancedSearch(fieldname,value,operation,join);
						}
					}
				}
				
			}
			if(!dates.isEmpty()){
				for (String keyed : dates.keySet()) {
					if(!dates.get(keyed).equals("")){
						asq += " ( "+dates.get(keyed)+" )  "+ operation;	
					}
				}
			}
			
			if(!asq.equals("")){
				asq = asq.substring(0, asq.length()-(operation.length() + 2));
			}
			asq = asq.trim();
			if(!asq.equals("")){
				String asquery = "( "+asq+" )";
				restriction.put("advancedsearch", asquery);
			}
			key="results";

			returndata = getJSON(storage,restriction,key,base);
			ui.sendJSONResponse(returndata);
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

	private String getAdvancedSearch(String fieldname, String value, String operator, String join){
		if(!value.equals("")){
			try{
				FieldSet fieldSet = this.r.getFieldFullList(fieldname);
				String section = fieldSet.getSection(); 	// Get the payload part
				String spath=this.r.getServicesRecordPath(section);
				String[] parts=spath.split(":",2);
				// Replace user wildcards with service-legal wildcards
				if(value.contains("*")){
					value = value.replace("*", "%");
					join = " ilike ";
				}
				String fieldSpecifier = getSearchSpecifierForField(fieldname, fieldSet);
				log.debug("Built XPath specifier for field: " + fieldname + " is: "+fieldSpecifier);
				
				return parts[0]+":"+fieldSpecifier+join+"\""+value +"\""+ " " + operator+ " ";
			}
			catch(Exception e){
				log.error("Problem creating advanced search specifier for field: "+fieldname);
				log.error(e.getLocalizedMessage());
				return "";
			}
		}
		return "";
	}


	/**
	 * Returns an NXQL-conformant string that specifies the full (X)path to this field.
	 * May recurse to handle nested fields.
	 * This should probably live in Field.java, not here.
	 * 
	 * @param fieldname the name of the field
	 * @param fieldSet the containing fieldSet
	 * @return NXQL conformant specifier.
	 **/
	private String getSearchSpecifierForField(String fieldname, FieldSet fieldSet ) {
		String specifier = fieldname;	// default is just the simple field name
		
		// Check for a composite (fooGroupList/fooGroup). For these, the name is the 
		// leaf, and the first part is held in the "services parent"
		if(fieldSet.hasServicesParent()) {
			// Prepend the services parent field, and make the child a wildcard
			String [] svcsParent = fieldSet.getServicesParent();
			if(svcsParent[0] != null && !svcsParent[0].isEmpty()) {
				specifier = svcsParent[0] +"/*"; 
			}
		}
		
		FieldParent parent = fieldSet.getParent();

		boolean isRootLevelField = false;			// Assume we are recursing until we see otherwise
		if(parent instanceof Record) {	// A simple reference to base field.
			isRootLevelField = true;
			log.debug("Specifier for root-level field: " + fieldname + " is: "+specifier);
		} else {
			FieldSet parentFieldSet = (FieldSet)parent;
			// "repeator" marks things for some expansion - not handled here (?)
			if(parentFieldSet.getSearchType().equals("repeator")) {
				isRootLevelField = true;
			} else {
				// Otherwise, we're dealing with some amount of nesting.
				// First, recurse to get the fully qualified path to the parent.
				String parentID = parentFieldSet.getID();
				log.debug("Recursing for parent: " + parentID );
				specifier = getSearchSpecifierForField(parentID, parentFieldSet);
							
				// Is parent a scalar list or a complex list?
				Repeat rp = (Repeat)parentFieldSet;
				FieldSet[] children = rp.getChildren("");
				int size = children.length;
				// HACK - we should really mark a repeating scalar as such, 
				// or a complex schema from which only 1 field is used, will break this.
				if(size > 1){
					// The parent is a complex schema, not just a scalar repeat
					// Append the field name to build an XPath-like specifier.
					specifier += "/"+fieldname;
				} else{
					// Leave specifier as is. We just search on the parent name,
					// as the backend is smart about scalar lists. 
				}
			}
			log.debug("Specifier for non-leaf field: " + fieldname + " is: "+specifier);
		}
		if(isRootLevelField) {
			// TODO - map leaf names like "titleGroupList/titleGroup" to "titleGroupList/*"
		}
		return specifier;
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
			if(paths[i].startsWith(mybase+"/"))
				paths[i]=paths[i].substring((mybase+"/").length());
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
