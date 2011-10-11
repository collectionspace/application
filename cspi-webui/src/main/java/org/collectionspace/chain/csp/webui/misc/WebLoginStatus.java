/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLoginStatus  implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebLoginStatus.class);
	private Spec spec;
	
	public WebLoginStatus(Spec spec) {
		this.spec = spec;
	}

	private JSONObject getPermissions(Storage storage) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException {
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
				String resourceName = Generic.ResourceNameUI(spec,active.getJSONObject(j).getString("resourceName"));
				JSONArray permissions = Generic.PermissionLevelArray(active.getJSONObject(j).getString("actionGroup"));
				perms.put(resourceName, permissions);
			}
			//currently you can' assign authority vocabulary permissions uniquely so they are munged here for the UI 
			//eventually you will need to pivot from /personauthorities/{csid} to the vocab instance
			//this currently sets the perms of the instance to that of the auth
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
	
	public void testlogin(Request in) throws UIException {
		UIRequest request=in.getUIRequest();
		try{
			Storage storage = in.getStorage();
			JSONObject output= new JSONObject();
			if(request.getSession() != null && request.getSession().getValue(UISession.USERID) != null ){
				if(request.getSession().getValue(UISession.USERID).equals("")){
					output.put("login", false);
				}				
				else{
					JSONObject perms = getPermissions(storage);
					if(perms.has("permissions")){
						output.put("permissions",perms.getJSONObject("permissions"));
						output.put("csid",perms.getString("csid"));
						output.put("screenName",perms.getString("screenName"));
						output.put("userId",perms.getString("userId"));
						output.put("login", true);
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
