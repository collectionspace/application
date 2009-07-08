/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.schema;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

/** Schema (UI Spec) retrieval API. A bit pointless as the impl is only a stub anyway, and this API will change 
 * immeasurably shortly.
 * 
 */
public interface SchemaStore {

	public abstract JSONObject getSchema(String path) throws IOException,
			JSONException;

}