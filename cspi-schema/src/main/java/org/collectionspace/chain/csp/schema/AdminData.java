/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * all admin specific data from the cspace-config.xml file that is parsed when
 * the server starts up
 * 
 * Everything the App layer needs to complete it's admin functions
 * 
 * @author caret
 * 
 */
public class AdminData {
	String username, password, tenant, tenantname;
	Integer lifeInMinsOfCookie;
	int termListCacheAge = 0;
	int autocompleteListCacheAge = 0;
	int reportListCacheAge = 0;
	int uploadedMediaCacheAge = 0;
	int uiSpecSchemaCacheAge = 0;
	int uiStaticHTMLResourcesCacheAge = 0; 
	int uiStaticCSSResourcesCacheAge = 0; 
	int uiStaticJSResourcesCacheAge = 0; 
	int uiStaticMediaResourcesCacheAge = 0; 
	int uiStaticPropertiesResourcesCacheAge = 0; 

	public AdminData(Spec spec, ReadOnlySection section) {
		username = (String) section.getValue("/username");
		password = (String) section.getValue("/password");
		tenant = (String) section.getValue("/tenant");
		tenantname = (String) section.getValue("/tenantname");
		String stringMinutes = (String) section.getValue("/cookievalidformins");
		if (stringMinutes.equals("")) {
			stringMinutes = "30";
		}
		lifeInMinsOfCookie = Integer.parseInt(stringMinutes);
		
		stringMinutes = (String) section.getValue("/termlist-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			termListCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/autocompletelist-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			autocompleteListCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/reportlist-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			reportListCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/uploaded-media-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uploadedMediaCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/uispecschema-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiSpecSchemaCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/ui-html-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiStaticHTMLResourcesCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/ui-css-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiStaticCSSResourcesCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/ui-js-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiStaticJSResourcesCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/ui-media-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiStaticMediaResourcesCacheAge = Integer.parseInt(stringMinutes);
		}

		stringMinutes = (String) section.getValue("/ui-props-cache-timeout");
		if (!StringUtils.isEmpty(stringMinutes)) {
			uiStaticPropertiesResourcesCacheAge = Integer.parseInt(stringMinutes);
		}
	}

	/*
	 * XXX hopefully the service layer will change so we don't have to have
	 * login details hardcoded
	 */
	public String getAuthUser() {
		return username;
	}

	public String getTenant() {
		return tenant;
	}
	public String getTenantName() {
		return tenantname;
	}

	public String getAuthPass() {
		return password;
	}

	public Integer getCookieLife() {
		return lifeInMinsOfCookie;
	}

	public int getTermListCacheAge() {
		return termListCacheAge;
	}

	public int getAutocompleteListCacheAge() {
		return autocompleteListCacheAge;
	}

	public int getReportListCacheAge() {
		return reportListCacheAge;
	}

	public int getUploadedMediaCacheAge() {
		return uploadedMediaCacheAge;
	}

	public int getUiSpecSchemaCacheAge() {
		return uiSpecSchemaCacheAge;
	}

	public int getUiStaticHTMLResourcesCacheAge() {
		return uiStaticHTMLResourcesCacheAge;
	}

	public int getUiStaticCSSResourcesCacheAge() {
		return uiStaticCSSResourcesCacheAge;
	}

	public int getUiStaticJSResourcesCacheAge() {
		return uiStaticJSResourcesCacheAge;
	}

	public int getUiStaticMediaResourcesCacheAge() {
		return uiStaticMediaResourcesCacheAge;
	}

	public int getUiStaticPropertiesResourcesCacheAge() {
		return uiStaticPropertiesResourcesCacheAge;
	}

	public AdminData getAdminData() {
		return this;
	}
	

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		record.put("cookieLife", lifeInMinsOfCookie);
		record.put("admin_username", username);
		out.put("AdminData", record);
	}
}
