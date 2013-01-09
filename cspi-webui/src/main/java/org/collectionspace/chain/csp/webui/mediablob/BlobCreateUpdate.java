package org.collectionspace.chain.csp.webui.mediablob;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.misc.Generic;
import org.collectionspace.chain.csp.webui.record.RecordCreateUpdate;
import org.collectionspace.chain.csp.webui.record.RecordRead;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobCreateUpdate   extends RecordCreateUpdate  {
	private static final Logger log=LoggerFactory.getLogger(BlobCreateUpdate.class);
	
	public BlobCreateUpdate(Record r,Boolean create) {
		super(r,create);
	}
	

	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException { // REM - Documentation here would be nice
		try {
			JSONObject data=new JSONObject();
			JSONObject data2=new JSONObject();
			JSONObject data3=new JSONObject();
			data2.put("fileName", request.getFileName());
			data2.put("getbyteBody", request.getbyteBody());
			data2.put("contentType", "multipart/form-data");
			data.put("fields", data2);
			if(create) {
				path=sendJSON(storage,null,data,null);
				data3.put("file",  path);
				String[] parts = path.split("/");
				String csid = parts[parts.length -1];
				data3.put("csid",csid);
			}			
			//readblob metadata

			JSONObject data7=reader.getJSON(storage,data3.getString("csid"));
			data7.put("file", path);
			data7.put("csid", data3.get("csid"));
			request.sendJSONResponse(data7);
			request.setOperationPerformed(Operation.OK);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (Exception x) {
			throw new UIException(x);
		}	
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}

	public void configure(WebUI ui,Spec spec) {
		//upload_dest=ui.getUploadDest();
	}
}
