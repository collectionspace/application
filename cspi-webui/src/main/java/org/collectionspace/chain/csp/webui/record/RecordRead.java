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

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
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

public class RecordRead implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RecordRead.class);
	private String base;
	private Record record;

	private RecordSearchList searcher;
	private RecordAuthorities termsused;
	private RecordRelated relatedobj;
	private Spec spec;
	private boolean record_type;
	private boolean authorization_type;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public RecordRead(Record r) { 
		this.base=r.getID();
		this.spec=r.getSpec();
		this.record = r;
		this.searcher = new RecordSearchList(r,false);
		this.termsused = new RecordAuthorities(r);
		record_type=r.isType("record");
		authorization_type=r.isType("authorizationdata");
	}
		

	
	private JSONObject createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject recordtypes=new JSONObject();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		//loop over all procedure/recordtypes
		for(Record thisr : spec.getAllRecords()) {
			if(thisr.isType("record")||thisr.isType("procedure")){

				this.relatedobj = new RecordRelated(this.record,thisr);
				this.relatedobj.configure(spec);
				recordtypes = this.relatedobj.getRelations(storage, restrictions,recordtypes);
				
			}
		}
		
		return recordtypes;
	}
	


	
	
	private JSONArray getPermissions(Storage storage,JSONObject activePermissions) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException{
		JSONArray set = new JSONArray();
		JSONObject testset = new JSONObject();
		
		log.info(activePermissions.toString());
		//we are ignoring pagination so this will return the first 40 roles only
		//UI doesn't know what it wants to do about pagination etc
		//mark active roles
		if(activePermissions.has("permission"))
		{
			JSONArray active = activePermissions.getJSONArray("permission");
			for(int j=0;j<active.length();j++){
				testset.put(active.getJSONObject(j).getString("resourceName"),active.getJSONObject(j));
			}
		}

		JSONObject temp = new JSONObject();
		
		//get all permissions
		int pageNum = 0;
		JSONObject permrestrictions = new JSONObject();
		permrestrictions.put("queryTerm", "actGrp");
		permrestrictions.put("queryString", "CRUDL");
		permrestrictions.put("pageNum", Integer.toString(pageNum));
		String permbase = spec.getRecordByWebUrl("permission").getID();
		JSONObject returndata = searcher.getJSON(storage,permrestrictions,"items",permbase);
		while(returndata.has("items") && returndata.getJSONArray("items").length()>0){

			//merge active and nonactive
			JSONArray items = returndata.getJSONArray("items");
			for(int i=0;i<items.length();i++){
				JSONObject item = items.getJSONObject(i);
				JSONObject permission = new JSONObject();
				String resourcename = item.getString("summary");
				permission.put("resourceName", Generic.ResourceNameUI(spec, resourcename));
				String permlevel =  "none";
				

				if(resourcename.endsWith("/*/workflow/")){
					String resourcename1 = resourcename.substring(1);
					String resource = resourcename1.split("\\/\\*\\/workflow\\/")[0]; 
					String tempres = Generic.ResourceNameUI(spec, resource);
					if(testset.has(resourcename)){
						permlevel = Generic.PermissionLevelString(testset.getJSONObject(resourcename1).getString("actionGroup"));
					
						if(permlevel.equals("delete")){
							JSONObject permission2 = new JSONObject();
							permission2.put("resourceName", tempres);
							permission2.put("permission", permlevel);
							temp.put(tempres, permission2);
						}
						
					}
					
				}
				
				if(testset.has(resourcename) && !temp.has(resourcename)){
					permlevel = Generic.PermissionLevelString(testset.getJSONObject(resourcename).getString("actionGroup"));

					permission.put("permission", permlevel);
					temp.put(resourcename, permission);
				}
				
			}
			
			
			pageNum++;
			permrestrictions.put("pageNum", Integer.toString(pageNum));
			returndata = searcher.getJSON(storage,permrestrictions,"items",permbase);
			
		}

		
		
		//change soft workflow to main Delete
		Iterator rit=temp.keys();
		while(rit.hasNext()) {
			String key=(String)rit.next();
			Object value = temp.get(key);

			set.put(value);
		}
		
		return set;
	}
	
	/* Wrapper exists to decomplexify exceptions: also used inCreateUpdate, hence not private */
	JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		JSONObject restrictions=new JSONObject();
		try {
			if(record_type || authorization_type) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid,restrictions);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				out.put("csid",csid);
				out.put("fields",fields);
				if(base.equals("role")){
					if(authorization_type){
						JSONObject permissions = storage.retrieveJSON(base+"/"+csid+"/"+"permroles/",restrictions);
						JSONArray allperms = getPermissions(storage,permissions);
						fields.put("permissions",allperms);
					}
				}
				else{
					JSONArray tusd = this.termsused.getTermsUsed(storage, base+"/"+csid, new JSONObject());
					JSONObject relations=createRelations(storage,csid);
					out.put("relations",relations);
					out.put("termsUsed",tusd);
				}
				
			} else {
				out=storage.retrieveJSON(base+"/"+csid,restrictions);
			}
		} catch (ExistException e) {
			throw new UIException("JSON Not found ",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException x) {
			// XXX dan to fix exception handling during Nov 2010
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		} catch (JSONException e) {
			throw new UIException("Could not create JSON",e);
		}
		if (out == null) {
			throw new UIException("No JSON Found");
		}
		return out;
	}
	
	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		// Get the data
		JSONObject outputJSON = getJSON(storage,path);
		try {
			outputJSON.put("csid",path);
		} catch (JSONException e1) {
			throw new UIException("Cannot add csid",e1);
		}
		// Write the requested JSON out
		request.sendJSONResponse(outputJSON);
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
	public void configure(Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
