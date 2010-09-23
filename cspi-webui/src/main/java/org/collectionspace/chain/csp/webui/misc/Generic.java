package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Generic {

	/**
	 * CSPACE-2894
	 * make permission names match the UI names when the app sends the data to the UI
	 * @param spec
	 * @param servicename
	 * @return
	 */
	public static String ResourceName(Spec spec, String servicename){
		try{
			Record test = spec.getRecordByServicesUrl(servicename);
			return test.getWebURL();
		}
		catch(Exception e){
			return servicename;
		}
	}

	/**
	 * CSPACE-2913
	 * 	permissions: {
	 * 		loansin: ["read", "create", "delete", "update", "list"], 
	 *  	intakes: ["read", "list"],
	 *  	...
	 * 	} 
	 * @param actGrp
	 * @return
	 */
	public static JSONArray PermissionLevelArray(String actGrp){
		JSONArray level = new JSONArray();
		if(actGrp.contains("C")){
			level.put("create");			
		}
		if(actGrp.contains("R")){
			level.put("read");			
		}
		if(actGrp.contains("U")){
			level.put("update");			
		}
		if(actGrp.contains("D")){
			level.put("delete");			
		}
		if(actGrp.contains("L")){
			level.put("list");			
		}
		
		return level;
	}
	

	public static String PermissionLevelString(String actGrp){
		String level = "none";
		if(actGrp.equals("CRUDL")){
			level="delete";
		}
		else if(actGrp.equals("CRUL")){
			level="write";
		}
		else if(actGrp.equals("RL")){
			level="read";
		}
		
		return level;
	}
	

	/**
	 * create role array
	 * [{roleId:"","roleName":"",roleSelected:"true/false"},{ ... }]
	 * @param activeRoles
	 * @return
	 * @throws UnderlyingStorageException 
	 * @throws UnimplementedException 
	 * @throws ExistException 
	 * @throws JSONException 
	 * @throws UIException 
	 */
	public static JSONArray getRoles(Storage storage,JSONObject activeRoles) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException{
		JSONObject set = new JSONObject();
		JSONArray roles = new JSONArray();
		JSONObject testset = new JSONObject();
		//get all roles - actually dont

	//	JSONObject rolerestrictions = new JSONObject();
	//	String rolebase = spec.getRecordByWebUrl("role").getID()+"/";
	//	JSONObject returndata = searcher.getJSON(storage,rolerestrictions,"items",rolebase);
		
		//mark active roles
		if(activeRoles.has("role"))
		{
			JSONArray active = activeRoles.getJSONArray("role");
			for(int j=0;j<active.length();j++){
				active.getJSONObject(j).put("roleSelected", "true");
				String roleId = active.getJSONObject(j).getString("roleId");

				String[] ids=roleId.split("/");
				active.getJSONObject(j).put("roleId", ids[ids.length - 1]);
	//			testset.put(active.getJSONObject(j).getString("roleId"),active.getJSONObject(j));
			}
			roles = active;
		}
		
		//merge active and nonactive
		/*
		JSONArray items = returndata.getJSONArray("items");
		for(int i=0;i<items.length();i++){
			JSONObject item = items.getJSONObject(i);
			JSONObject role = new JSONObject();
			String roleId = item.getString("csid");
			if(testset.has(roleId)){
				item.put("roleSelected", "true");
			}
			else{
				item.put("roleSelected", "false");
			}
			roles.put(item);
		}
		*/
		
		
		

		//we are ignoring pagination so this will return the first 40 roles only
		//UI doesn't know what it wants to do about pagination etc
		return roles;
	}
}
