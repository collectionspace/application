/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.record;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesInitialize;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebMethodWithOps;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.misc.Generic;
import org.collectionspace.chain.csp.webui.nuispec.CacheTermList;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.helper.core.ResponseCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordCreateUpdate implements WebMethodWithOps {
	private static final Logger log=LoggerFactory.getLogger(RecordCreateUpdate.class);
	protected String url_base,base;
	protected boolean create;
	protected Record record;
	protected AuthoritiesVocabulariesInitialize avi;
	protected Spec spec;
	protected RecordRead reader;
	protected RecordSearchList searcher;
	protected CacheTermList ctl;
	
	protected static final String BLOBS_SERVICE_URL_PATTERN = "/cspace-services/blobs/";
	
	public RecordCreateUpdate(Record r,boolean create) { 
		this.spec=r.getSpec();
		this.record = r;
		this.url_base=r.getWebURL();
		this.base=r.getID();
		this.create=create;
		this.reader=new RecordRead(r);
		this.avi = new AuthoritiesVocabulariesInitialize(r,false);
		this.reader.configure(spec);
		this.searcher = new RecordSearchList(r,RecordSearchList.MODE_LIST);
	}
		
	private void deleteAllRelations(Storage storage,String csid) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject r=new JSONObject();
		r.put("src",base+"/"+csid);	
		// XXX needs pagination support CSPACE-1819
		JSONObject data = storage.getPathsJSON("relations/main",r);
		String[] paths = (String[]) data.get("listItems");
		for(String relation : paths) {
			storage.deleteJSON("relations/main/"+relation);
		}
	}
	
	private void setRelations(Storage storage,String csid,JSONArray relations) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		deleteAllRelations(storage,csid);
		for(int i=0;i<relations.length();i++) {
			// Extract data from miniobject
			JSONObject in=relations.getJSONObject(i);
			String dst_type=spec.getRecordByWebUrl(in.getString("recordtype")).getID();
			String dst_id=in.getString("csid");
			String type=in.getString("relationshiptype");
			// Create relation
			JSONObject r=new JSONObject();
			r.put("src",base+"/"+csid);
			r.put("dst",dst_type+"/"+dst_id);
			r.put("type",type);
			storage.autocreateJSON("relations/main",r,null);
		}
	}
	
	public String sendJSON(Storage storage, String path, JSONObject data, JSONObject restrictions)
			throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		final String WORKFLOW_TRANSITION = "workflowTransition";
		final String WORKFLOW_TRANSITION_LOCK = "lock";
		
		JSONObject fields=data.optJSONObject("fields");
		JSONArray relations=data.optJSONArray("relations");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path, fields, restrictions);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(base, fields, restrictions);
		}
		if(relations!=null)
			setRelations(storage,path,relations);
		if(record.supportsLocking() && data.has(WORKFLOW_TRANSITION)
				&& WORKFLOW_TRANSITION_LOCK.equalsIgnoreCase(data.getString(WORKFLOW_TRANSITION))) {
			// If any problem, will throw exception.
			storage.transitionWorkflowJSON(base+"/"+path, WORKFLOW_TRANSITION_LOCK);
		}
		return path;
	}
			
	private JSONObject permJSON(String permValue) throws JSONException{
		JSONObject perm = new JSONObject();
		perm.put("name", permValue);
		return perm;
	}
	
	private JSONObject getPerm(Storage storage, Record recordForPermResource,
									String resourceName, String permLevel) 
							throws JSONException, ExistException, UnimplementedException, 
									UnderlyingStorageException, UIException {
		JSONObject permitem = new JSONObject();

		if(permLevel.equals("none")) {
			return permitem;
		}

		JSONArray actions =  new JSONArray();
		JSONObject permR = permJSON("READ");
		JSONObject permC = permJSON("CREATE");
		JSONObject permU = permJSON("UPDATE");
		JSONObject permD = permJSON("DELETE");
		JSONObject permL = permJSON("SEARCH");

		///cspace-services/authorization/permissions?res=acquisition&actGrp=CRUDL
		String queryString = "CRUDL";
		
		if(permLevel.equals(Generic.READ_PERMISSION)) {
			queryString = "RL";
			actions.put(permR);
			actions.put(permL);
		} else if(permLevel.equals(Generic.WRITE_PERMISSION)) {
			queryString = "CRUL";
			actions.put(permC);
			actions.put(permR);
			actions.put(permU);
			actions.put(permL);
		} else if(permLevel.equals(Generic.DELETE_PERMISSION) 
				|| permLevel.equals(Generic.LOCK_PERMISSION)) {
			actions.put(permC);
			actions.put(permR);
			actions.put(permU);
			if(recordForPermResource!=null && recordForPermResource.hasSoftDeleteMethod()) {
				// Delete is handled in the workflow perms
				queryString = "CRUL";
			} else {
				queryString = "CRUDL";
				actions.put(permD);
			}
			actions.put(permL);		// Keep this here to preserve CRUDL order of actions
		} else {
			log.warn("RecordCreateDelete.getPerm passed unknown permLevel: " + permLevel);
		}

		String permbase = spec.getRecordByWebUrl("permission").getID();
		
		permitem = getPermID(storage, Generic.ResourceNameServices(spec, resourceName), queryString, permbase, actions);
		return permitem;
	}
	
	private JSONObject getWorkflowPerm(Storage storage, Record recordForPermResource,
					String resourceName, String permLevel, String workflowTransition) 
							throws JSONException, ExistException, UnimplementedException, 
									UnderlyingStorageException, UIException {
		JSONObject permitem = new JSONObject();

		if(permLevel.equals("none")){
			return permitem;
		}
		
		JSONArray actions =  new JSONArray();
		JSONObject permC = permJSON("CREATE");
		JSONObject permR = permJSON("READ");
		JSONObject permU = permJSON("UPDATE");
		JSONObject permD = permJSON("DELETE");
		JSONObject permL = permJSON("SEARCH");
		actions.put(permR);
		actions.put(permL);

		///cspace-services/authorization/permissions?res=acquisition&actGrp=CRUDL
		String queryString = "RL";
		String permbase = spec.getRecordByWebUrl("permission").getID();
		boolean hasRights = false;
		
		if(workflowTransition.equals(DELETE_WORKFLOW_TRANSITION)) {
			// permLevel delete or lock includes this
			hasRights = permLevel.equals(Generic.DELETE_PERMISSION)
						|| permLevel.equals(Generic.LOCK_PERMISSION);
		} else if(workflowTransition.equals(LOCK_WORKFLOW_TRANSITION)) {
			// permLevel lock includes this
			// UI does not yet support admin of the lock perm, so 
			//hasRights = permLevel.equals(Generic.LOCK_PERMISSION);
			// Assumes this is only called for records that actually support locking...
			hasRights = permLevel.equals(Generic.DELETE_PERMISSION)
					|| permLevel.equals(Generic.UPDATE_PERMISSION)
					|| permLevel.equals(Generic.LOCK_PERMISSION);
		} else {
			log.warn("RecordCreateUpdate.getWorkflowPerm passed unknown workflowTransition: "
						+workflowTransition);
		}
		if(hasRights) {
			actions.put(permC);
			actions.put(permU);
			actions.put(permD);
			// They do not really get DELETE rights on a workflow, but that is what the services models by
			// default, so let's stick with that 
			queryString = "CRUDL";
		}
		// Workflow resources all have leading slashes.
		String resource = Generic.ResourceNameServices(spec, resourceName)+WORKFLOW_SUB_RESOURCE+workflowTransition;
		if(!resource.startsWith("/"))
			resource = "/"+resource;
		permitem = getPermID(storage,resource,queryString, permbase, actions);
		return permitem;
	}
	

	private JSONObject getPermID(Storage storage, String name, String queryString, String permbase, JSONArray actions) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException{

		JSONObject permitem = new JSONObject();
		JSONObject permrestrictions = new JSONObject();
		permrestrictions.put("keywords", name);
		permrestrictions.put("queryTerm", "actGrp");
		permrestrictions.put("queryString", queryString);
		JSONObject data = searcher.getJSON(storage,permrestrictions,"items",permbase);

		String permid = "";
		JSONArray items = data.getJSONArray("items");
		for(int i=0;i<items.length();i++){
			JSONObject item = items.getJSONObject(i);
			String resourcename = item.getString("summary");
			String actionGroup = item.getString("number");
			//need to do a double check as the query is an inexact match
			if(resourcename.equals(name) && actionGroup.equals(queryString)){
				permid = item.getString("csid");
			}
		}
		if(permid.equals("")){

			//create the permission
			/**
			 * {
"effect": "PERMIT",
"resourceName": "testthing2",

"action":[{"name":"CREATE"},{"name":"READ"},{"name":"UPDATE"},{"name":"DELETE"},{"name":"SEARCH"}]
}
			 */
					
			JSONObject permission_add = new JSONObject();
			permission_add.put("effect", "PERMIT");
			permission_add.put("description", "created because we couldn't find a match");
			permission_add.put("resourceName", name);
			permission_add.put("actionGroup", queryString);
			permission_add.put("action", actions);

			permid=storage.autocreateJSON(spec.getRecordByWebUrl("permission").getID(),permission_add,null);
			
		}
		
		if(!permid.equals("")){
			permitem.put("resourceName", name);
			permitem.put("permissionId", permid);
			permitem.put("actionGroup", queryString);
		}
		return permitem;

	}
	

	private void assignTerms(Storage storage, String path, JSONObject data) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, UIException{
		JSONObject fields=data.optJSONObject("fields");
		String insId = "";
		
		if(fields.has("terms")){
			Record vr = this.spec.getRecord("vocab");
			Record thisr = spec.getRecord("vocab");
			String sid = fields.getString("shortIdentifier");
			String name = fields.getString("displayName");
			insId =  "vocab-"+sid;
			if(create){
				Map<String,String> options=new HashMap<String,String>();
				options.put("id", insId);
				options.put("title", name);
				options.put("web-url", sid);
				options.put("title-ref", sid);

				Instance ins=new Instance(thisr, options);
				vr.addInstance(ins);
			}
			ctl.get(storage, sid,vr,0);
		}

	}

	private final static String DELETE_WORKFLOW_TRANSITION = Generic.DELETE_PERMISSION;
	private final static String LOCK_WORKFLOW_TRANSITION = Generic.LOCK_PERMISSION;
	private static final String PUBLISH_URL_SUFFIX = "publish";

	private void assignPermissions(Storage storage, String path, JSONObject data) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, UIException{
		JSONObject fields=data.optJSONObject("fields");
			
		JSONArray permdata = new JSONArray();
		JSONObject permcheck = new JSONObject();
		if(fields.has("permissions")){
			JSONArray permissions = fields.getJSONArray("permissions");
			for(int i=0;i<permissions.length();i++){
				JSONObject perm = permissions.getJSONObject(i);

				Record recordForPermResource = Generic.RecordNameServices(spec,perm.getString("resourceName"));
				if(recordForPermResource!=null ) {
					if( recordForPermResource.hasSoftDeleteMethod()){
						JSONObject permitem = getWorkflowPerm(storage, recordForPermResource, 
													perm.getString("resourceName"), perm.getString("permission"),
													DELETE_WORKFLOW_TRANSITION);
						if(permitem.has("permissionId")){
							if(permcheck.has(permitem.getString("resourceName"))){
								//ignore as we have duplicate name - eek
								log.warn("RecordCreateUpdate.assignPermissions got duplicate workflow/delete permission for: "
											+ permitem.getString("resourceName"));
							}else{
								permcheck.put(permitem.getString("resourceName"), permitem);
								permdata.put(permitem);
							}
						}
					}
					if( recordForPermResource.supportsLocking() ){
						JSONObject permitem = getWorkflowPerm(storage, recordForPermResource, 
													perm.getString("resourceName"), perm.getString("permission"),
													LOCK_WORKFLOW_TRANSITION);
						if(permitem.has("permissionId")){
							if(permcheck.has(permitem.getString("resourceName"))){
								//ignore as we have duplicate name - eek
								log.warn("RecordCreateUpdate.assignPermissions got duplicate workflow/lock permission for: "
											+ permitem.getString("resourceName"));
							}else{
								permcheck.put(permitem.getString("resourceName"), permitem);
								permdata.put(permitem);
							}
						}
					}
				}
				JSONObject permitem = getPerm(storage, recordForPermResource, perm.getString("resourceName"),perm.getString("permission"));
				if(permitem.has("permissionId")){
					if(permcheck.has(permitem.getString("resourceName"))){
						//ignore as we have duplicate name - eek
						log.warn("RecordCreateUpdate.assignPermissions got duplicate permission for: "
								+ permitem.getString("resourceName"));
					}else{
						permcheck.put(permitem.getString("resourceName"), permitem);
						permdata.put(permitem);
					}
				}
			}
		}

		//log.info("permdata"+permdata.toString());
		JSONObject roledata = new JSONObject();
		roledata.put("roleName", fields.getString("roleName"));

		String[] ids=path.split("/");
		roledata.put("roleId", ids[ids.length - 1]);
		
		

		JSONObject accountrole = new JSONObject();
		JSONObject arfields = new JSONObject();
		arfields.put("role", roledata);
		arfields.put("permission", permdata);
		accountrole.put("fields", arfields);
		//log.info("WAAA"+arfields.toString());
		if(fields!=null)
			path=storage.autocreateJSON(spec.getRecordByWebUrl("permrole").getID(),arfields,null);
	}
	
	private boolean setPayloadField(String fieldName, JSONObject payloadOut, JSONObject fieldsSrc, JSONObject dataSrc, String defaultValue) throws JSONException {
		boolean result = true;
		
		if (fieldsSrc != null && fieldsSrc.has(fieldName)) {
			payloadOut.put(fieldName, fieldsSrc.getString(fieldName));
		} else if (dataSrc != null && dataSrc.has(fieldName)) {
			payloadOut.put(fieldName, dataSrc.getString(fieldName));
		} else if (defaultValue != null) {
			payloadOut.put(fieldName, defaultValue);
		} else {
			result = false;
		}
		
		return result;
	}
	
	/*
	 * Set a field in the payloadOut object with a value from fieldSrc or if not present there then from dataSrc
	 */
	private boolean setPayloadField(String fieldName, JSONObject payloadOut, JSONObject fieldsSrc, JSONObject dataSrc) throws JSONException {
		return setPayloadField(fieldName, payloadOut, fieldsSrc, dataSrc, null);
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject restrictions = new JSONObject();
			JSONObject data=request.getJSONBody();

			if (this.base.equals("role")) {
				JSONObject fields=data.optJSONObject("fields");
				if((fields.optString("roleName") == null || fields.optString("roleName").equals("")) && fields.optString("displayName") !=null){
					String test  = fields.optString("displayName");
					test = test.toUpperCase();
					test.replaceAll("\\W", "_");
					fields.put("roleName", "ROLE_"+test);
					data.put("fields", fields);
				}
				// If we are updating a role, then we need to clear the userperms cache
				// Note that creating a role does not impact things until we assign it
				if(!create) {
					ResponseCache.clearCache(ResponseCache.USER_PERMS_CACHE);
				}
			}
			
			if (this.record.getID().equals("media")) {
				JSONObject fields=data.optJSONObject("fields");
				// Handle linked media references
				if (!fields.has("blobCsid") || StringUtils.isEmpty(fields.getString("blobCsid"))) {	// If has blobCsid, already has media link so do nothing more
					// No media, so consider the source
					// "sourceUrl" is not a declared field in the app layer config, but the UI passes it in
					// Can consider mapping srcUri to this if want to clean that up
					if (fields.has("sourceUrl")){
						// We have a source - see where it is from
						String uri = fields.getString("sourceUrl");
						if(uri.contains(BLOBS_SERVICE_URL_PATTERN)) {
							// This is an uploaded blob, so just pull the csid and set into blobCsid
							String[] parts = uri.split(BLOBS_SERVICE_URL_PATTERN);	// Split to get CSID
							String[] bits = parts[1].split("/"); // Strip off anything trailing the CSID
							fields.put("blobCsid",bits[0]);
						} else { // This must be an external Url source
							// External Source is handled as params to the CREATE/UPDATE of the media record
							restrictions.put(Record.BLOB_SOURCE_URL, uri);
							// Tell the Services to delete the original after creating derivatives
							restrictions.put(Record.BLOB_PURGE_ORIGINAL, Boolean.toString(true));
						}
						fields.remove("sourceUrl");
						data.put("fields", fields);
					}
				}
			}
			
			if (this.record.getID().equals("output")) {
				//
				// Invoke a report
				//
				ReportUtils.invokeReport(this, storage, request, path);
			} else {
				//
				// <Please document this clause.>
				//
				FieldSet displayNameFS = this.record.getDisplayNameField();
				String displayNameFieldName = (displayNameFS!=null)?displayNameFS.getID():null;
				boolean remapDisplayName = false;
				String remapDisplayNameValue = null;
				boolean quickie = false;
				if(create) {
					quickie = (data.has("_view") && data.getString("_view").equals("autocomplete"));
					remapDisplayName = quickie && !"displayName".equals(displayNameFieldName);
					// Check to see if displayName field needs remapping from UI
					if(remapDisplayName) {
						// Need to map the field for displayName, and put it into a proper structure
						JSONObject fields = data.getJSONObject("fields");
						remapDisplayNameValue = fields.getString("displayName");
						if(remapDisplayNameValue != null) {
							// This needs generalizing, in case the remapped name is nested
							/*
							 * From vocab handling where we know where the termDisplayName is
							FieldSet parentTermGroup = (FieldSet)displayNameFS.getParent();
							JSONArray parentTermInfoArray = new JSONArray();
							JSONObject termInfo = new JSONObject();
							termInfo.put(displayNameFieldName, remapDisplayNameValue);
							parentTermInfoArray.put(termInfo);
							*/
							fields.put(displayNameFieldName, remapDisplayNameValue);
							fields.remove("displayName");
						}
					}
					path=sendJSON(storage, null, data, restrictions); // REM - We needed a way to send query params, so I'm adding "restrictions" here
					data.put("csid",path);
					data.getJSONObject("fields").put("csid",path);
					// Is this needed???
					/*
					String refName = data.getJSONObject("fields").getString("refName");
					data.put("urn", refName);
					data.getJSONObject("fields").put("urn", refName);
					// This seems wrong - especially when we create from existing.
					if(remapDisplayName){
						JSONObject newdata = new JSONObject();
						newdata.put("urn", refName);
						newdata.put("displayName",quickieDisplayName);
						data = newdata;
					}
					 */
				} else {
					path=sendJSON(storage,path,data,restrictions);
				}
				
				if (path == null) {
					throw new UIException("Insufficient data for create (no fields?)");
				}

				if (this.base.equals("role")) {
					assignPermissions(storage, path, data);
				}
				if (this.base.equals("termlist")) {
					assignTerms(storage, path, data);
				}
				
				data=reader.getJSON(storage,path); // We do a GET now to read back what we created.
				if(quickie){
					JSONObject newdata = new JSONObject();
					JSONObject fields = data.getJSONObject("fields");
					String displayName = fields.getString(remapDisplayName?displayNameFieldName:"displayName");
					newdata.put("displayName",remapDisplayNameValue);
					String refName = fields.getString("refName");
					newdata.put("urn", refName);
					data = newdata;
				}

				request.sendJSONResponse(data);
				request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
				if(create)
					request.setSecondaryRedirectPath(new String[]{url_base,path});
			}
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			UIException uiexception =  new UIException(x.getMessage(),0,"",x);
			request.sendJSONResponse(uiexception.getJSON());
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}catch (Exception x) {
			throw new UIException(x);
		}
	
	}
	
	@Override
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	
	@Override
	public void configure(WebUI ui,Spec spec) {
		this.searcher.configure(spec);
	}

	@Override
	public Operation getOperation() {
		return create ? Operation.CREATE : Operation.UPDATE;
	}

	@Override
	public String getBase() {
		return base;
	}

	@Override
	public Spec getSpec() {
		// TODO Auto-generated method stub
		return spec;
	}
}
