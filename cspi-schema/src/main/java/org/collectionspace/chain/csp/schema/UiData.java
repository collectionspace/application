/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.json.JSONException;
import org.json.JSONObject;

public class UiData {
	String baseurl;

	public UiData(Spec spec, ReadOnlySection section) {
		baseurl=(String)section.getValue("/baseurl");
	}

	public String getBaseURL() { return baseurl; }

  public UiData getUiData() { return this; }

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		record.put("baseurl", baseurl);
		out.put("UiData", record);
	}
}
