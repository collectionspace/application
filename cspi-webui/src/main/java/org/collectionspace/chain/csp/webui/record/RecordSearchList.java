package org.collectionspace.chain.csp.webui.record;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
	private JSONObject pathsToJSON(Storage storage,String base,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		/*
		if(base.equals("permission")){
			//get all permissions (argh pagination)
			JSONObject restriction = new JSONObject();
			JSONArray allpaths = getfullList(storage,restriction);

			for(int i=0;i<allpaths.length();i++) {
				members.put(generateEntry(storage,base,allpaths.getString(i)));
			}
			out.put(key,members);
			JSONObject grouped = groupPermissions(members);
			//check if we have all the permissions we need
			checkPermissions(grouped, storage);
			out.put("groupedPermissions", grouped);
		}
		else{
		*/
			for(String p : paths)
				members.put(generateEntry(storage,base,p));
			out.put(key,members);
			/*
		}
		*/
		return out;
	}
	
	
	/**
	 * Check whether all the permissions are included in the list, if a permission isn't, add it
	 * @param {JSONObject} permissions JSON containing grouped permissions
	 * @return {boolean} Whether the grouped permissions contain the needed permissions
	 * @throws JSONException 
	 * @throws UnderlyingStorageException 
	 * @throws UnimplementedException 
	 * @throws ExistException 
	 */
	private void checkPermissions(JSONObject permissions, Storage storage) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{
		//get all the records from the default xml
		Record[] records = r.getSpec().getAllRecords();

		//this list should probably be moved to the default xml
		String[] resourceNames = {"id","idgenerators","/idgenerators/*/ids","vocabularies","vocabularyitems","/vocabularies/*/items/","locationauthorities","/locationauthorities/*/items/","locations","relations","relations/subject/*/type/*/object/*","contacts","notes"};

		//loop over all types of records in the default.xml
		for(Record record : records){
			if(record.isType("record")){
				addOrCreatePermissions(permissions, storage,record.getID());
			}
		}
		for(String s : resourceNames){
			addOrCreatePermissions(permissions, storage, s);
		}
	}

	/**
	 * Check whether or not the permissions for the resource name/record name exist and create them if not
	 * @param {JSONObject} permissions A list of permissions that are already created
	 * @param {Storage} storage The storage for this type of record (e.g. AuthorizationStorage for permission)
	 * @param resourceName The resourceName for which the permissions need to be checked/created
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	private void addOrCreatePermissions(JSONObject permissions, Storage storage, String resourceName) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException{
		String[] actions = {"read","update","delete","none"};

		//check whether this type of record is available in the grouped permissions
		if(permissions.has(resourceName)){
			JSONArray resourcelist = permissions.getJSONArray(resourceName);
			//check whether the record type has the four permissions (read, update, delete, none)
			// need to check whether it has the right 4 permissions
			//if(resourcelist.length() < 4){
				//find the missing permission
				List<String> missing = new ArrayList<String>();
				boolean exists = false;
				for(String action : actions){
					for(int i=0, il=resourcelist.length();i<il;i++){
						JSONObject resource = resourcelist.getJSONObject(i);
						if(resource.has(action)){
							exists = true;
							break;
						}else{
							exists = false;
						}
					}
					if(!exists){
						missing.add(action);
					}
				}
				for(String s : missing){
					//save the permission in the database
					JSONObject ac = new JSONObject();
					ac = createMissingPermissions(s, resourceName, storage);
					
					//save the permission in the grouped permissions
					resourcelist.put(ac);
				}
			//}
		}else{
			JSONArray actionlist = new JSONArray();
			for(String s : actions){
				JSONObject ac = createMissingPermissions(s, resourceName, storage);
				actionlist.put(ac);
			}
			//add the record and its permissions
			permissions.put(resourceName, actionlist);
		}
	}
	
	private JSONObject createMissingPermissions(String s, String name, Storage storage) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException{
		//might need to change to a hash if they need more complex permissions
		List<String> actions = new ArrayList<String>(Arrays.asList("search","read","create","update","delete","none"));

		/*
		 * SEARCH=16;READ=1|:CREATE=2;UPDATE=4|;DELETE=8|;
		 * read = SEARCH,READ = 9
		 * update = SEARCH,READ,CREATE,UPDATE = 23
		 * delete = SEARCH,READ,CREATE,UPDATE,DELETE =31 
		 */
		List<String> actionsforpermission = new ArrayList<String>();
		
		//create the new permission JSON
		JSONObject permission = new JSONObject();
		permission.put("resourceName", name);
		permission.put("effect", "PERMIT");
		
		if(!s.equals("none")){
			for(int i=0,il=actions.indexOf(s);i<=il;i++){
				actionsforpermission.add(actions.get(i));
			}
		}
		
		JSONArray actionlist = new JSONArray();
		for(String a : actionsforpermission){
			JSONObject newaction = new JSONObject();
			newaction.put("name", a.toUpperCase());
			JSONArray namelist = new JSONArray();
			namelist.put(newaction);
			JSONObject action = new JSONObject();
			action.put("action", namelist);
			actionlist.put(action);
		}
		permission.put("actions",actionlist);
		
		//add the missing permission in the service layer
		String csid = storage.autocreateJSON(r.getID(), permission);
		//add the permission in the grouped permissions list
		JSONObject ac = new JSONObject();
		ac.put(s, csid);
		return ac;
	}
	
	/**
	 * Group all the permissions by their recordType
	 * @param {JSONArray} permissionlist A list of permissions containing for each permission: resourceName, csid, effect
	 * @return {JSONObject} A list of grouped permissions per resourceName (e.g. all permissions for intake, loansout,...)
	 * @throws JSONException
	 */
	private JSONObject groupPermissions(JSONArray permissionlist) throws JSONException{
		//create a JSONObject to contain all of the grouped permissions
		JSONObject permission = new JSONObject();
		
		//loop over all the permissions that are coming in
		for(int i=0,il=permissionlist.length();i<il;i++){
			JSONObject li = permissionlist.getJSONObject(i);
			
			//get the resourceName
			String name = li.getString("summary");
			
			//depending on the action number, save it as a read/write/delete/none
			String action="";
			Integer actionint = li.getInt("action");
			/*
			 * READ=1:CREATE=2;UPDATE=4;DELETE=8;SEARCH=16
			 * update = READ,CREATE,UPDATE,SEARCH = 23
			 * delete = READ,CREATE,UPDATE,DELETE,SEARCH =31 
			 * read = READ,SEARCH = 9
			 */
			switch(li.getInt("action")){
				case 9:
					action = "read";
					break;
				case 23:
					action="update";
					break;
				case 31:
					action="delete";
					break;
				case 0:
					action="none";
					break;
				default:
					action="unknown";
					break;
			}
			
			//save the permission (read,write,delete,none) and its csid
			JSONObject perm = new JSONObject();
			perm.put(action, li.getString("csid"));
			
			//get all the existing permissions for the current resourceName and save them in an array
			JSONArray permArray = new JSONArray();
			if(permission.has(name)){
				for(int j=0,jl=permission.getJSONArray(name).length();j<jl;j++){
					permArray.put(permission.getJSONArray(name).get(j));
				}
			}

			//ignore unknown
			if(!action.equals("unknown")){
				//add the new permission to the array
				permArray.put(perm);
			}
			
			//save the resourceName and its new permissions
			permission.put(name, permArray);
			
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
