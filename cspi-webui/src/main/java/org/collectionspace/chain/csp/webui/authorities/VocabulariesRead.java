/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
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


public class VocabulariesRead implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(VocabulariesRead.class);
	private Instance n;
	private String base;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public VocabulariesRead(Instance n) {
		this.base=n.getID();
		this.n=n;
	}
	
	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}

	/**
	 * Returns all the Authorities that are associated to a vocabulary item
	 * @param storage
	 * @param path
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getTermsUsed(Storage storage,String path) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject mini=storage.retrieveJSON(path+"/authorityrefs", new JSONObject());
		JSONArray out=new JSONArray();
		Iterator t=mini.keys();
		while(t.hasNext()) {
			String field=(String)t.next();
			if(mini.get(field) instanceof JSONArray){
				JSONArray array = (JSONArray)mini.get(field);
				for(int i=0;i<array.length();i++) {
					JSONObject in = array.getJSONObject(i);
					JSONObject entry=getTermsUsedData(in);
					out.put(entry);
				}
			}
			else{
				JSONObject in=mini.getJSONObject(field);
				JSONObject entry=getTermsUsedData(in);
				out.put(entry);
			}
		}
		return out;
	}	
	
	private JSONObject getTermsUsedData(JSONObject in) throws JSONException{
		JSONObject entry=new JSONObject();
		entry.put("csid",in.getString("csid"));
		entry.put("recordtype",in.getString("recordtype"));
		//entry.put("sourceFieldName",field);
		entry.put("sourceFieldselector",in.getString("sourceFieldselector"));
		entry.put("sourceFieldName",in.getString("sourceFieldName"));
		entry.put("sourceFieldType",in.getString("sourceFieldType"));
		
		entry.put("number",in.getString("displayName"));
		return entry;
	}
	
	
	
	/**
	 * Returns all the objects that are linked to a vocabulary item
	 * @param storage
	 * @param path
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getRefObj(Storage storage,String path) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONArray out=new JSONArray();
		JSONObject mini = storage.retrieveJSON(path+"/refObjs", new JSONObject());
		if(mini != null){
			Iterator t=mini.keys();
			while(t.hasNext()) {
				String field=(String)t.next();
				JSONObject in=mini.getJSONObject(field);
				/*
				JSONObject entry=new JSONObject();
				entry.put("csid",in.getString("csid"));
				entry.put("recordtype",in.getString("sourceFieldType"));
				entry.put("sourceFieldName",field);
				entry.put("number",in.getString("sourceFieldName"));
				*/
				out.put(in);
			}
		}
		
		return out;
	}
	
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view", new JSONObject());
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;
	}

	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject in=storage.retrieveJSON("relations/main/"+csid, new JSONObject());
		String[] dstid=in.getString("dst").split("/");
		String type=in.getString("type");
		JSONObject mini=generateMiniRecord(storage,dstid[0],dstid[1]);
		mini.put("relationshiptype",type);
		mini.put("relid",in.getString("csid"));
		return mini;
	}
	
	private JSONObject createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject recordtypes=new JSONObject();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		// XXX needs pagination support CSPACE-1819
		JSONObject data = storage.getPathsJSON("relations/main",restrictions);
		String[] relations = (String[]) data.get("listItems");
		for(String r : relations) {
			try {
				JSONObject relateitem = generateRelationEntry(storage,r);
				String type = relateitem.getString("recordtype");
				if(!recordtypes.has(type)){
					recordtypes.put(type, new JSONArray());
				}
				recordtypes.getJSONArray(type).put(relateitem);
			} catch(Exception e) {
				// Never mind.
			}
		}
		return recordtypes;
	}
	
	/* Wrapper exists to decomplexify exceptions: also used inCreateUpdate, hence not private */
	JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			String refPath = n.getRecord().getID()+"/"+n.getTitleRef()+"/";
			JSONObject fields=storage.retrieveJSON(refPath+csid, new JSONObject());
			csid = fields.getString("csid");
			//fields.put("csid",csid);
			//JSONObject relations=createRelations(storage,csid);
			out.put("csid",csid);
			out.put("fields",fields);
			out.put("relations",new JSONArray());
			//out.put("relations",relations);
			out.put("termsUsed",getTermsUsed(storage,refPath+csid));
			out.put("refobjs",getRefObj(storage,refPath+csid));
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		} catch (JSONException e) {
			throw new UIException("Could not create JSON"+e,e);
		}
		if (out == null) {
			throw new UIException("No JSON Found");
		}
		return out;
	}
	
	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		// Get the data
		JSONObject outputJSON = getJSON(storage,path);
	
		// Write the requested JSON out
		request.sendJSONResponse(outputJSON);
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
