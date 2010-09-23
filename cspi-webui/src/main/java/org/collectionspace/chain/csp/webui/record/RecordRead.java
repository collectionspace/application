package org.collectionspace.chain.csp.webui.record;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
	private Spec spec;
	private boolean record_type;
	private boolean authorization_type;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	public RecordRead(Record r) { 
		this.base=r.getID();
		this.spec=r.getSpec();
		this.record = r;
		this.searcher = new RecordSearchList(r,false);
		record_type=r.isType("record");
		authorization_type=r.isType("authorizationdata");
	}
		
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view/relate");
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;
	}

	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject in=storage.retrieveJSON("relations/main/"+csid);
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
	
	@SuppressWarnings("unchecked")
	private JSONArray getTermsUsed(Storage storage,String path) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {

		JSONArray out=new JSONArray();
		if(record.hasTermsUsed()){

			JSONObject mini=storage.retrieveJSON(path+"/refs");

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
	
	
	private JSONArray getPermissions(Storage storage,JSONObject activePermissions) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException{
		JSONArray set = new JSONArray();
		JSONObject testset = new JSONObject();
		//get all permissions
		JSONObject permrestrictions = new JSONObject();
		permrestrictions.put("queryTerm", "actGrp");
		permrestrictions.put("queryString", "CRUDL");
		String permbase = spec.getRecordByWebUrl("permission").getID();
		JSONObject returndata = searcher.getJSON(storage,permrestrictions,"items",permbase);

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
		

		//merge active and nonactive
		JSONArray items = returndata.getJSONArray("items");
		for(int i=0;i<items.length();i++){
			JSONObject item = items.getJSONObject(i);
			JSONObject permission = new JSONObject();
			String resourcename = item.getString("summary");
			permission.put("resourceName", resourcename);
			String permlevel =  "none";
			if(testset.has(resourcename)){
				permlevel = Generic.PermissionLevelString(testset.getJSONObject(resourcename).getString("actionGroup"));
			}
			permission.put("permission", permlevel);
			set.put(permission);
		}
		
		
		return set;
	}
	
	/* Wrapper exists to decomplexify exceptions: also used inCreateUpdate, hence not private */
	JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			if(record_type || authorization_type) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				JSONObject relations=createRelations(storage,csid);
				out.put("csid",csid);
				out.put("fields",fields);
				out.put("relations",relations);
				out.put("termsUsed",getTermsUsed(storage,base+"/"+csid));
				if(authorization_type && base.equals("role")){
					JSONObject permissions = storage.retrieveJSON(base+"/"+csid+"/"+"permroles/");
					JSONArray allperms = getPermissions(storage,permissions);
					fields.put("permissions",allperms);
				}
			} else {
				out=storage.retrieveJSON(base+"/"+csid);
			}
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem getting",e);
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
