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
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesInitialize;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordCreateUpdate implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RecordCreateUpdate.class);
	protected String url_base,base;
	protected boolean create;
	protected Record record;
	protected AuthoritiesVocabulariesInitialize avi;
	protected Spec spec;
	protected RecordRead reader;
	protected RecordSearchList searcher;
	protected CacheTermList ctl;
	
	public RecordCreateUpdate(Record r,boolean create) { 
		this.spec=r.getSpec();
		this.record = r;
		this.url_base=r.getWebURL();
		this.base=r.getID();
		this.create=create;
		this.reader=new RecordRead(r);
		this.avi = new AuthoritiesVocabulariesInitialize(r,false);
		this.reader.configure(spec);
		this.searcher = new RecordSearchList(r,false);
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
			storage.autocreateJSON("relations/main",r);
		}
	}
	
	public String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		JSONArray relations=data.optJSONArray("relations");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(base,fields);
		}
		if(relations!=null)
			setRelations(storage,path,relations);
		return path;
	}
			
	private JSONObject permJSON(String permValue) throws JSONException{
		JSONObject perm = new JSONObject();
		perm.put("name", permValue);
		return perm;
	}
	
	private JSONObject getPerm(Storage storage, String resourceName, String permlevel, Boolean isWorkflow) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, UIException{

		Record r = Generic.RecordNameServices(spec,resourceName);
		JSONObject permitem = new JSONObject();
		JSONArray actions =  new JSONArray();
		JSONArray wf_actions =  new JSONArray();
		JSONObject permR = permJSON("READ");
		JSONObject permC = permJSON("CREATE");
		JSONObject permU = permJSON("UPDATE");
		JSONObject permD = permJSON("DELETE");
		JSONObject permL = permJSON("SEARCH");

		///cspace-services/authorization/permissions?res=acquisition&actGrp=CRUDL
		String queryString = "CRUDL";
		String wf_querystring = "";

		
		if(permlevel.equals("none")){
			queryString = "";
			actions = new JSONArray();
			return permitem;
		}
		if(permlevel.equals("read")){
			queryString = "RL";
			actions.put(permR);
			actions.put(permL);
		}
		if(permlevel.equals("write")){
			queryString = "CRUL";
			actions.put(permC);
			actions.put(permR);
			actions.put(permU);
			actions.put(permL);
		}
		if(permlevel.equals("delete")){
			if(r!=null && r.hasSoftDeleteMethod()){
				queryString = "CRUL";
				actions.put(permC);
				actions.put(permR);
				actions.put(permU);
				actions.put(permL);
				wf_querystring = "CRUDL";
				wf_actions.put(permC);
				wf_actions.put(permR);
				wf_actions.put(permU);
				wf_actions.put(permD);
				wf_actions.put(permL);
			}
			else{
				queryString = "CRUDL";
				actions.put(permC);
				actions.put(permR);
				actions.put(permU);
				actions.put(permD);
				actions.put(permL);
			}
		}

		String permbase = spec.getRecordByWebUrl("permission").getID();
		
		if(isWorkflow){

			if(r!=null && r.hasSoftDeleteMethod()){
				if(wf_querystring.equals("")){
					wf_querystring = "RL";
					wf_actions.put(permR);
					wf_actions.put(permL);
				}
				
				permitem = getPermID(storage, Generic.ResourceNameServices(spec, resourceName)+"/*/workflow/", wf_querystring, permbase, wf_actions);
			}
		}
		else{
			permitem = getPermID(storage, Generic.ResourceNameServices(spec, resourceName), queryString, permbase, actions);
		}
		return permitem;
	}
	

	private JSONObject getPermID(Storage storage, String name, String queryString, String permbase, JSONArray actions) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException{

		JSONObject permitem = new JSONObject();
		JSONObject wf_permrestrictions = new JSONObject();
		wf_permrestrictions.put("keywords", name);
		wf_permrestrictions.put("queryTerm", "actGrp");
		wf_permrestrictions.put("queryString", queryString);
		JSONObject data = searcher.getJSON(storage,wf_permrestrictions,"items",permbase);

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

			permid=storage.autocreateJSON(spec.getRecordByWebUrl("permission").getID(),permission_add);
			
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

	private void assignPermissions(Storage storage, String path, JSONObject data) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException, UIException{
		JSONObject fields=data.optJSONObject("fields");
			
		JSONArray permdata = new JSONArray();
		JSONObject permcheck = new JSONObject();
		if(fields.has("permissions")){
			JSONArray permissions = fields.getJSONArray("permissions");
			for(int i=0;i<permissions.length();i++){
				JSONObject perm = permissions.getJSONObject(i);

				Record r = Generic.RecordNameServices(spec,perm.getString("resourceName"));
				//if(r!=null){
					if(r!=null && r.hasSoftDeleteMethod()){
						JSONObject permitem = getPerm(storage,perm.getString("resourceName"),perm.getString("permission"),true);
						if(permitem.has("permissionId")){
							if(permcheck.has(permitem.getString("resourceName"))){
								//ignore as we have duplicate name - eek
							}else{
								permcheck.put(permitem.getString("resourceName"), permitem);
								permdata.put(permitem);
							}
						}
					}
					JSONObject permitem = getPerm(storage,perm.getString("resourceName"),perm.getString("permission"),false);
					if(permitem.has("permissionId")){
						if(permcheck.has(permitem.getString("resourceName"))){
							//ignore as we have duplicate name - eek
						}else{
							permcheck.put(permitem.getString("resourceName"), permitem);
							permdata.put(permitem);
						}
					}
				//}
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
			path=storage.autocreateJSON(spec.getRecordByWebUrl("permrole").getID(),arfields);
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=request.getJSONBody();

			if(this.base.equals("role")){
				JSONObject fields=data.optJSONObject("fields");
				if((fields.optString("roleName") == null || fields.optString("roleName").equals("")) && fields.optString("displayName") !=null){
					String test  = fields.optString("displayName");
					test = test.toUpperCase();
					test.replaceAll("\\W", "_");
					fields.put("roleName", "ROLE_"+test);
					data.put("fields", fields);
				}
			}
			if(this.record.getID().equals("media")){
				JSONObject fields=data.optJSONObject("fields");
				if(fields.has("srcUri")){
					
					//is this internal or external?
					//XXX HACK as ervice layer having issues with external urls
					String uri = fields.getString("srcUri");
					/*
					String baseurl = "http://nightly.collectionspace.org:8180/cspace-services/blobs/";
					if(uri.startsWith(baseurl)){
						uri = uri.replace(baseurl, "");
						String[] parts = uri.split("/");
						fields.put("blobCsid",parts[0]);
						fields.remove("srcUri");
					}
					*/
					String[] parts = uri.split("/blobs/");
					String[] bits = parts[1].split("/");
					fields.put("blobCsid",bits[0]);
					fields.remove("srcUri");
					data.put("fields", fields);
				}
			}

			if(this.record.getID().equals("output")){
				//do a read instead of a create as reports are special and evil

				JSONObject fields=data.optJSONObject("fields");
				JSONObject payload = new JSONObject();
				payload.put("mode", "single");

				if(fields.has("mode")){
					payload.put("mode", fields.getString("mode"));
				}
				if(fields.has("docType")){
					String type = spec.getRecordByWebUrl(fields.getString("docType")).getServicesTenantSg();
					payload.put("docType", type);
				}
				if(fields.has("singleCSID")){
					payload.put("singleCSID", fields.getString("singleCSID"));
				}
				else if(fields.has("groupCSID")){
					payload.put("singleCSID", fields.getString("csid"));
				}
				
				JSONObject out=storage.retrieveJSON(base+"/"+path,payload);

				byte[] data_array = (byte[])out.get("getByteBody");
				request.sendUnknown(data_array,out.getString("contenttype"));
				
				//request.sendJSONResponse(out);
				request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			}
			else{
				if(create) {
					path=sendJSON(storage,null,data);
					data.put("csid",path);
					data.getJSONObject("fields").put("csid",path);
				} else
					path=sendJSON(storage,path,data);
				if(path==null)
					throw new UIException("Insufficient data for create (no fields?)");

				if(this.base.equals("role")){
					assignPermissions(storage,path,data);
				}
				if(this.base.equals("termlist")){
					assignTerms(storage,path,data);
				}
				
				data=reader.getJSON(storage,path);
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
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	
	public void configure(WebUI ui,Spec spec) {}
}
