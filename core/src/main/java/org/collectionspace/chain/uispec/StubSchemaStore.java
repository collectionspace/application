/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.uispec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/** Schema (aka UI Spec) storage hack. Temporarily UI Specs are sotred on the filesystem.
 */
public class StubSchemaStore implements SchemaStore {
	private File schema_root;
	
	public StubSchemaStore(String root) {
		schema_root=new File(root,"uispecs");
		if(!schema_root.exists())
			schema_root.mkdir();
	}
	
	/* (non-Javadoc)
	 * @see org.collectionspace.chain.schema.SchemaStore#getSchema(java.lang.String)
	 */
	public JSONObject getSchema(String path) throws IOException, JSONException {
		File schema=new File(schema_root,path);
		if(schema.exists() && schema.isDirectory()) {
			schema=new File(schema,"uispec.json");
		}
		if(!schema.exists()) {
			schema=new File(schema_root,path+".json");
		}
		if(!schema.exists())
			throw new IOException(path+" does not exist");
		FileInputStream stream=new FileInputStream(schema);
		String data=IOUtils.toString(stream);
		JSONObject out=new JSONObject(data);
		stream.close();
		return out;
	}
}
