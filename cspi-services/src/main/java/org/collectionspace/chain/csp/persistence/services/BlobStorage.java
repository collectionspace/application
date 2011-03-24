package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(RecordStorage.class);
	
	public BlobStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		super(r,conn);
		initializeGlean(r);
	}
	
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		

		ReturnedURL url = null;
		try {
		byte[] bitten = (byte[]) jsonObject.get("getbyteBody");
		String uploadname = jsonObject.getString("fileName");
		String type = jsonObject.getString("contentType");
		String path = r.getServicesURL();
			url = conn.getStringURL(RequestMethod.POST, path, bitten, uploadname, type, creds, cache);
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return url.getURLTail();
		//String bob = "";
		//return bob;
	}

}
