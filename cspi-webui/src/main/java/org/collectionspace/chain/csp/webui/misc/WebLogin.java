/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.AdminData;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ConflictException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnauthorizedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLogin implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebLogin.class);
	private static final String LOGIN_CONNECTION_ERR = "login-connectionError";
	private static final String LOGIN_CONFLICT_ERR = "login-conflictError";
	private static final String LOGIN_FAIL_ERR = "fail";
	private String login_dest,login_failed_dest,tenantid;
	private Spec spec;
	
	public WebLogin(WebUI ui, Spec spec) {
		this.spec=spec;
		this.tenantid = spec.getAdminData().getTenant();
	}

	/**
	 * If successful, returns null; otherwise, returns an error message.
	 * @param storage
	 * @param tenant
	 * @return
	 */
	private String loginAttempt(Storage storage, String tenant) {
		String result = LOGIN_FAIL_ERR; // null equals success
		
		try {
			String base = spec.getRecordByWebUrl("userperm").getID();
			JSONObject activePermissions = storage.retrieveJSON(base + "/0/", new JSONObject());

			// check tenant
			if (activePermissions.has("account")) {
				JSONObject acc = activePermissions.getJSONObject("account");
				if (acc.has("tenantId")) {
					if (acc.getString("tenantId").equals(tenant)) {
						result = null;
					}
				}
			}
		} catch (UnauthorizedException ue) {
			result = LOGIN_FAIL_ERR;
		} catch (ConflictException e) {
			result = LOGIN_CONFLICT_ERR;
		} catch (Exception e) {
			result = LOGIN_CONNECTION_ERR;
	}
		
		return result;
	}
	
	private void login(Request in) throws UIException { // Temporary hack for Mars
		UIRequest request=in.getUIRequest();
		String username=request.getRequestArgument(USERID_PARAM);
		String password=request.getRequestArgument(PASSWORD_PARAM);
		String tenantId=tenantid;
	
		if (username ==  null) {
			JSONObject data = new JSONObject();
			if (request.isJSON()) {
				data=request.getJSONBody();
			} else {
				data=request.getPostBody();
			}
			//
			// Stop defaulting to GET request when UI layer stops doing login via GET
			if (data.has("userid")) {
				try {
					username=data.getString("userid");
					password=data.getString("password");
					if(data.has("tenant")){
						tenantId=data.getString("tenant");
					}
				} catch (JSONException e) {
					username=request.getRequestArgument(USERID_PARAM);
					password=request.getRequestArgument(PASSWORD_PARAM);
				}
			}
		}
		
		UISession uiSession = request.getSession();
		uiSession.setValue(UISession.USERID,username);
		uiSession.setValue(UISession.PASSWORD,password);
		uiSession.setValue(UISession.TENANT,tenantId);
		in.reset();
		
		String logingErrMsg = loginAttempt(in.getStorage(), tenantId);
		if (logingErrMsg == null) {
			try {
				/*
				 * If enabled, this code would attempt to initialize/reload the default authorities and term lists.  It would attempt to
				 * do this with the credentials just used to successfully login.  If the credentials did not suffice to perform the init/reload
				 * then the user would be redirected to an error page rather than the default post-login landing page.
				 * 
				 * This may be a safer (better?) approach then the current one.  The current approach uses the tenant admin credentials stored
				 * in the Application layer's config.  Since keeping these credentials in the config is a security vulnerability, we may need
				 * stop using them and rely on this apporach for init/reloading the default authorities and term lists.
				 * 
				WebReset webReset = new WebReset(false, false);
				webReset.configure(ui, spec);
				webReset.run(in, new String[0], false);
				*/
			} catch (Throwable t) {
				log.error(t.getMessage());
				throw t;
			}
			request.setRedirectPath(login_dest.split("/"));
		} else {
			log.error(String.format("Login attempt to tenant '%s' with username '%s' failed.",
					tenantId, username));
			uiSession.setValue(UISession.USERID,"");  // REM - 2/7/2013: If we got here that means we failed to authenticate with the Services (or another "storage" container), so I would think we should kill any existing session and not just null out the username and password fields.
			uiSession.setValue(UISession.PASSWORD,"");
			uiSession.setValue(UISession.TENANT,"");
			request.setRedirectPath(login_failed_dest.split("/"));
			request.setRedirectArgument("result", logingErrMsg);
		}
	}
		
	public void run(Object in,String[] tail) throws UIException {
		login((Request)in);
	}

	public void configure() throws ConfigException {}

	public void configure(WebUI ui,Spec spec) {
		login_dest=ui.getLoginDest();
		login_failed_dest=ui.getLoginFailedDest();
	}
}
