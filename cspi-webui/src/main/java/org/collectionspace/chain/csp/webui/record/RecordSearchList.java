package org.collectionspace.chain.csp.webui.record;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public RecordSearchList(Record r,boolean search) {
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
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view");
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
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,base,p));
		out.put(key,members);
		if(base.equals("permission"))
			out.put("groupedPermissions", groupPermissions(members));
		return out;
	}
	
	/**
	 * Group all the permissions by their recordType
	 * @param {JSONArray} permissionlist A list of permissions containing for each permission: resourceName, csid, effect
	 * @return {JSONObject} A list of grouped permissions per resourceName (e.g. all permissions for intake, loansout,...)
	 * @throws JSONException
	 */
	private JSONObject groupPermissions(JSONArray permissionlist) throws JSONException{
		JSONObject permission = new JSONObject();
		for(int i=0,il=permissionlist.length();i<il;i++){
			JSONObject li = permissionlist.getJSONObject(i);
			String name = li.getString("summary");
			String[] parts = name.split("_");
			
			
			JSONObject perm = new JSONObject();
			perm.put(parts[0], li.getString("csid"));
			JSONArray permArray = new JSONArray();
			if(permission.has(parts[1])){
				for(int j=0,jl=permission.getJSONArray(parts[1]).length();j<jl;j++){
					permArray.put(permission.getJSONArray(parts[1]).get(j));
				}
			}
			permArray.put(perm);
			permission.put(parts[1], permArray);
		}
		return permission;
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
	private void search_or_list(Storage storage,UIRequest ui,String param, String pageSize, String pageNum) throws UIException {
		try {
			JSONObject restriction=new JSONObject();
			String key="items";
			if(param!=null) {
				restriction.put("keywords",param);
				key="results";
			}
			if(pageSize!=null) {
				restriction.put("pageSize",pageSize);
			}
			if(pageNum!=null) {
				restriction.put("pageNum",pageNum);
			}
			JSONObject data = storage.getPathsJSON(base,restriction);
			String[] paths = (String[]) data.get("listItems");
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
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"),q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null,q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
