/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import javax.servlet.http.HttpSession;

import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
//import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.collectionspace.csp.helper.core.ResponseCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

public class WebLoginStatus  implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebLoginStatus.class);
	private Spec spec;
	private Ehcache userPermsCache = ResponseCache.getCache(ResponseCache.USER_PERMS_CACHE);
	
	public WebLoginStatus(Spec spec) {
		this.spec = spec;
	}

	private JSONObject getPermissions(Storage storage) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException {
		final String WORKFLOW_DELETE_RESOURCE_TAIL = WORKFLOW_SUB_RESOURCE+"delete";
		final String WORKFLOW_LOCK_RESOURCE_TAIL = WORKFLOW_SUB_RESOURCE+"lock";
		JSONObject data = new JSONObject();
		JSONObject perms = new JSONObject();

		String permbase = "accountperms";//spec.getRecordByWebUrl("userperm").getID();
		String base = spec.getRecordByWebUrl("userperm").getID();
		JSONObject activePermissions = storage.retrieveJSON(base + "/0/", new JSONObject());

		//we are ignoring pagination so this will return the first 40 permissions only
		//UI doesn't know what it wants to do about pagination etc
		if(activePermissions.has("account"))
		{
			JSONObject account = activePermissions.getJSONObject("account");
			String csid = account.getString("accountId");
			String userId = account.getString("userId");
			String tenantId = account.getString("tenantId");
			String screenName = userId;
			if(account.has("screenName")){
				screenName = account.getString("screenName");
			}
			data.put("csid",csid);
			data.put("screenName",screenName);
			data.put("userId",userId);
			data.put("tenant",tenantId);
		}
		if(activePermissions.has("permission"))
		{
			JSONArray active = activePermissions.getJSONArray("permission");
			for(int j=0;j<active.length();j++){
				JSONObject perm = active.getJSONObject(j); 
				if(perm.has("resourceName") && perm.has("actionGroup")){
					JSONArray newPermissionsList = null;
					String resourceName = perm.getString("resourceName");
					String resourceNameUI = null;
					// Check and filter the workflow resources
					if(resourceName.endsWith(WORKFLOW_DELETE_RESOURCE_TAIL)) {
						// Only consider if we can write the workflow transition
						if(Generic.PermissionIncludesWritable(perm.getString("actionGroup"))) {
							if(resourceName.startsWith("/")){
								resourceName = resourceName.substring(1);
							}
							// Get the base resource that the workflow is related to
							String baseResource = resourceName.substring(0,resourceName.length()
														-WORKFLOW_DELETE_RESOURCE_TAIL.length()); 
							resourceNameUI = Generic.ResourceNameUI(spec, baseResource);
							// Workflow delete means we have CRUDL permissions
							if(!perms.has(resourceNameUI)) {
								newPermissionsList = Generic.PermissionLevelArray("CRUDL");
							} else {
								JSONArray prevPermsList = (JSONArray) perms.get(resourceNameUI);
								newPermissionsList = Generic.PermissionLevelArrayEnsure(prevPermsList, Generic.DELETE_PERMISSION);
							}
						}
					} else if(resourceName.endsWith(WORKFLOW_LOCK_RESOURCE_TAIL)) {
						// Only consider if we can write the workflow transition
						// TODO Should ignore this if record not configred to support locking.
						if(Generic.PermissionIncludesWritable(perm.getString("actionGroup"))) {
							if(resourceName.startsWith("/")){
								resourceName = resourceName.substring(1);
							}
							// Get the base resource that the workflow is related to
							String baseResource = resourceName.substring(0,resourceName.length()
														-WORKFLOW_LOCK_RESOURCE_TAIL.length()); 
							resourceNameUI = Generic.ResourceNameUI(spec, baseResource);
							// Workflow lock means we have CRUDL permissions too
							if(!perms.has(resourceNameUI)) {
								newPermissionsList = Generic.PermissionLevelArray("CRUDLK");
							} else {
								JSONArray prevPermsList = (JSONArray) perms.get(resourceNameUI);
								newPermissionsList = Generic.PermissionLevelArrayEnsure(prevPermsList, Generic.LOCK_PERMISSION);
							}
						}
					} else if(resourceName.endsWith(WORKFLOW_SIMPLE_SUB_RESOURCE)) {
						log.info("WebLoginStatus.getPerms: Ignoring resource for simple workflow subresource");
					} else if(resourceName.contains(WORKFLOW_SUB_RESOURCE)) {
						log.debug("WebLoginStatus.getPerms: Ignoring resource for unrecognized workflow transition: "
								+ resourceName);
					} else {
						// TODO - If the associated record is configured with soft-Delete, should we
						// filter 'D' from the actionGroup here?
						resourceNameUI = Generic.ResourceNameUI(spec, resourceName);
						newPermissionsList = Generic.PermissionLevelArray(perm.getString("actionGroup")); // Converts the "actionGroup" from the Services into a list of permission verbs
						if (perms.has(resourceNameUI) == true) {
							// Merge the existing permissions list with another.
							JSONArray prevPermsList = (JSONArray) perms.get(resourceNameUI);
							newPermissionsList = Generic.PermissionLevelArrayEnsure(prevPermsList, newPermissionsList);							
						}
					}
					
					if (newPermissionsList!=null) {
						perms.put(resourceNameUI, newPermissionsList);
					}
				}
			}
			// Services do not support permissions for individual namespaces within an authority,
			// e.g., to "Local Persons" vs. "ULAN Persons". 
			// This sets up permissions to each namespace (a.k.a. "instance" in this code) 
			// to match that of the parent authority.
			for (Record r: spec.getAllRecords()){
				if(r.isType("authority")){
					for(Instance ins: r.getAllInstances()){
						JSONArray permsdata = new JSONArray();
						if(r.getWebURL().equals("vocab")){
							if(perms.has("vocabularyitems")){
								permsdata = perms.getJSONArray("vocabularyitems");
							}
						}
						else{
							if(perms.has(r.getWebURL())){
								permsdata = perms.getJSONArray(r.getWebURL());
							}
						}
						perms.put(ins.getID(), permsdata);
					}
				}
			}
		}
		else{
			//no permissions = no roles for this tenant
			return new JSONObject();
			
		}
//		put resources with none permissions in

		for(Record r : spec.getAllRecords()) {
			if(!perms.has(r.getWebURL())){
				perms.put(r.getWebURL(), new JSONArray());
			}
		}
		
		//make termlist permissions match vocabularyItems permissions
		//perms.put("termlist", perms.get("vocab"));
		
		data.put("permissions",perms);
		return data;
	}
	

	private String buildUserPermsCacheKey(String userId, String tenantId) {
		return userId+":"+tenantId;
	}
	
	private JSONObject findPermsInCache(String userId, String tenantId) {
		JSONObject perms = null;
		// See if there is a cache of the permissions for this user and tenant.
		try {
			Element cacheHit = userPermsCache.get(buildUserPermsCacheKey(userId, tenantId));
			if(cacheHit != null) {
				perms = (JSONObject) cacheHit.getObjectValue();
			}
		} catch (IllegalStateException ise) {
			log.warn("WebLoginStatus - userperms cache not active: "+ise.getLocalizedMessage());
		} catch (CacheException ce) {
			log.warn("WebLoginStatus - userperms cache exception:"+ce.getLocalizedMessage());
		}
		return perms;
	}
	
	private void addPermsToCache(String userId, String tenantId, JSONObject perms) {
		// Update cache with the permissions for this user and tenant.
		try {
			userPermsCache.put(new Element(buildUserPermsCacheKey(userId, tenantId), perms));
		} catch (IllegalStateException ise) {
			log.warn("WebLoginStatus - userperms cache not active: "+ise.getLocalizedMessage());
		} catch (CacheException ce) {
			log.warn("WebLoginStatus - userperms cache exception:"+ce.getLocalizedMessage());
		} catch (IllegalArgumentException iae) {
			throw new RuntimeException("WebLoginStatus - tried to cache with null perms!:"
											+ iae.getLocalizedMessage());
		}
	}
	
	public void testlogin(Request in) throws UIException {
		UIRequest request=in.getUIRequest();
		try{
			Storage storage = in.getStorage();
			JSONObject output= new JSONObject();
			UISession uiSession = request.getSession();
			if(uiSession != null && uiSession.getValue(UISession.USERID) != null ){
				if(uiSession.getValue(UISession.USERID).equals("")){
					output.put("login", false);
				}				
				else{
					JSONObject perms = null;
					// See if there is a cache of the permissions for this user and tenant.
					String userId = (String) uiSession.getValue(UISession.USERID);
					String tenantId = (String) uiSession.getValue(UISession.TENANT);
					perms = findPermsInCache(userId, tenantId);
					boolean fFoundInCache;
					if(perms != null) {
						fFoundInCache = true;
					} else {
						fFoundInCache = false;
						perms = getPermissions(storage);
					}
					
					if(perms.has("permissions")){
						// Set the cache if did not find it there, and if it has perms
						// Will only slow down edge case of user with no roles.
						if(!fFoundInCache) {
							addPermsToCache(userId, tenantId, perms);
						}
						output.put("permissions",perms.getJSONObject("permissions"));
						output.put("csid",perms.getString("csid"));
						output.put("screenName",perms.getString("screenName"));
						output.put("userId",perms.getString("userId"));
						output.put("login", true);
						int maxInterval = 0;
						UIRequest uir = in.getUIRequest();
						if(uir != null) {
							HttpSession httpSession = request.getHttpSession();
							if(httpSession != null) {
								maxInterval = httpSession.getMaxInactiveInterval();
							}
						}
						// Need to consider the shorter of session timeout and cookie expiry.
						// cookie life is in minutes, so convert to seconds.
						int cookieLife = 60 * spec.getAdminData().getCookieLife();
						if(maxInterval == 0 || maxInterval >= cookieLife) {
							maxInterval = cookieLife;
						}
						output.put("maxInactive", maxInterval);
					}
					else{
						output.put("login", false);
						output.put("message", "no roles associated with this user");
					}
				}
			}
			else{
				output.put("login", false);
			}
			request.sendJSONResponse(output);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x.getMessage(),x);
		} catch (ExistException x) {
			//failed login test
			throw new UIException("Existence exception: ",x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: ",x);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
		
		
	}
	
	public void run(Object in,String[] tail) throws UIException {
		testlogin((Request)in);
	}

	public void configure(WebUI ui, Spec spec) {
		
	}
}
