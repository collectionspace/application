package org.collectionspace.chain.schema;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public interface SchemaStore {

	public abstract JSONObject getSchema(String path) throws IOException,
			JSONException;

}