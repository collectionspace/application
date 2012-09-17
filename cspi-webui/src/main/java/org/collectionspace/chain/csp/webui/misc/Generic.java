/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Generic {

	private static String tokensalt = "74102328Generic";
	
	private static final Logger log=LoggerFactory.getLogger(Generic.class);
/**
 * Function to create a hash for the record traverser functionality
 * @param csid
 * @return
 * @throws UIException 
 */
	public static String createHash(String csid) throws UIException  {
		try {
			byte[] buffer = csid.getBytes();
			byte[] result = null;
			StringBuffer buf = null;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			result = new byte[md5.getDigestLength()];
			md5.reset();
			md5.update(buffer);
			result = md5.digest(tokensalt.getBytes());
	
			//create hex string from the 16-byte hash 
			buf = new StringBuffer(result.length * 2);
				for (int i = 0; i < result.length; i++) {
					int intVal = result[i] & 0xff;
					if (intVal < 0x10) {
						buf.append("0");
					}
				buf.append(Integer.toHexString(intVal).toUpperCase());
			}
			return buf.toString().substring(0,32);
		} catch (NoSuchAlgorithmException e) {
			throw new UIException("There were problems with the algorithum");
		}
	}
	
	
	public static String getPermissionView(Spec spec, String servicename){
		try{
			Record test = null;
			//can we do a simple match
			if(spec.hasRecordByServicesUrl(servicename)){
				test = spec.getRecordByServicesUrl(servicename);
			}
			else{
				//else loop thr the records and see if we can do an auth match 
				for(Record r : spec.getAllRecords()) {
					if(r.isAuthorizationType(servicename)){
						test = r;
					}
				}
			}
			if(test.getAuthorizationView()){
				return "show";
			}
			return "none";
		}
		catch(Exception e){
			//do not display as we don't know what this is
			return "none";
		}
	}
	
	/**
	 * CSPACE-2894
	 * make permission names match the UI names when the app sends the data to the UI
	 * @param spec
	 * @param servicename
	 * @return
	 */
	public static String ResourceNameUI(Spec spec, String servicename){
		try{
			Record test = null;
			//can we do a simple match
			if(spec.hasRecord(servicename)){
				test = spec.getRecord(servicename);
			}
			else if(spec.hasRecordByServicesUrl(servicename)){
				test = spec.getRecordByServicesUrl(servicename);
			}
			else{
				//else loop thr the records and see if we can do an auth match 
				for(Record r : spec.getAllRecords()) {
					if(r.isAuthorizationType(servicename)){
						test = r;
					}
				}
			}
			return test.getWebURL();
		}
		catch(Exception e){
			return servicename;
		}
	}
	
	public static String ResourceNameServices(Spec spec, String uiname){
		try{
			Record test = spec.getRecordByWebUrl(uiname);
			return test.getAuthorizationType();
		}
		catch(Exception e){
			return uiname;
		}
	}
	
	public static Record RecordNameServices(Spec spec, String uiname){
		try{
			Record test = spec.getRecordByWebUrl(uiname);
			return test;
		}
		catch(Exception e){
			return null;
		}
	}
	
	public static String LOCK_PERMISSION = "lock";
	public static String DELETE_PERMISSION = "delete";
	public static String WRITE_PERMISSION = "write";
	public static String UPDATE_PERMISSION = "update";
	public static String READ_PERMISSION = "read";
	public static String LIST_PERMISSION = "list";
	public static String CREATE_PERMISSION = "create";
	public static String NONE_PERMISSION = "none";
	
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
			level.put(CREATE_PERMISSION);			
		}
		if(actGrp.contains("R")){
			level.put(READ_PERMISSION);			
		}
		if(actGrp.contains("U")){
			level.put(UPDATE_PERMISSION);			
		}
		if(actGrp.contains("D")){
			level.put(DELETE_PERMISSION);			
		}
		if(actGrp.contains("L")){
			level.put(LIST_PERMISSION);			
		}
		if(actGrp.contains("K")){
			level.put(LOCK_PERMISSION);			
		}
		
		return level;
	}
	
	public static JSONArray PermissionLevelArrayEnsure(JSONArray perms, String ensure){
		for(int i=0; i<perms.length();i++) {
			String toCheck;
			try {
				toCheck = perms.getString(i);
				if(ensure.equals(toCheck))
					return perms;
			} catch (JSONException e) {
				// Log and ignore - 
				log.error("Problem checking PermissionLevelArray: "+e.getLocalizedMessage());
			}
		}
		perms.put(ensure);
		return perms;
	}
		
	/*
	 * Merge a new list of permissions with an existing list of permissions
	 */
	public static JSONArray PermissionLevelArrayEnsure(JSONArray prevPermsList, JSONArray newPermsList) {
		JSONArray result = prevPermsList;
		
		for (int i = 0; i < newPermsList.length(); i++) {
			try {
				String newPerm = newPermsList.getString(i);
				PermissionLevelArrayEnsure(prevPermsList, newPerm);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.debug("Could not extract a permission from a permission list.", e);
			}
		}
		
		return result;
	}
	
	public static boolean PermissionIncludesWritable(String actGrp){
		return actGrp.contains("U");
	}

	public static String PermissionLevelString(String actGrp){
		String level = NONE_PERMISSION;
		if(actGrp.equals("CRUDL")){
			level = DELETE_PERMISSION;
		}
		else if(actGrp.equals("CRUL")){
			level = WRITE_PERMISSION;
		}
		else if(actGrp.equals("RL")){
			level = READ_PERMISSION;
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
				if(active.getJSONObject(j).length()!=0){

					active.getJSONObject(j).put("roleSelected", "true");
					String roleId = active.getJSONObject(j).getString("roleId");

					String[] ids=roleId.split("/");
					active.getJSONObject(j).put("roleId", ids[ids.length - 1]);
				}
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
