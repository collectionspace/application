package org.collectionspace.chain.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class StubSchemaStore implements SchemaStore {
	private String schema_root;
	
	public StubSchemaStore(String root) {
		schema_root=root;
	}
	
	/* (non-Javadoc)
	 * @see org.collectionspace.chain.schema.SchemaStore#getSchema(java.lang.String)
	 */
	public JSONObject getSchema(String path) throws IOException, JSONException {
		File schema=new File(schema_root,path);
		if(schema.exists() && schema.isDirectory()) {
			schema=new File(schema,"schema.json");
		}
		FileInputStream stream=new FileInputStream(schema);
		String data=IOUtils.toString(stream);
		JSONObject out=new JSONObject(data);
		stream.close();
		return out;
	}
}
