/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.json.JSONException;
import org.json.JSONObject;

public class ServicesIDGenerator implements ContextualisedStorage {
	private ServicesConnection conn;
	
	private static final Map<String,String> generators=new HashMap<String,String>();

	public ServicesIDGenerator(ServicesConnection conn, Spec spec) { 
		this.conn=conn;
		Record idgenerator = spec.getRecord("id");
		for(FieldSet fs: idgenerator.getAllFieldTopLevel("")){
			String name = fs.getID();
			String id = fs.getSelector();
			generators.put(name, id);
		}
	}
	
	@Override
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException,
		UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, 
			String filePath, String workflowTransition) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}
	
	/**
	 * This function generates a new id for the next inputted object.
	 * The id in the static list on top is the primary key in the generators_id table on the service layer.
	 */
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String val=conn.getTextDocument(RequestMethod.POST,"idgenerators/"+generators.get(filePath)+"/ids",null,creds,cache);
			JSONObject out=new JSONObject();
			out.put("next",val);
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSON exception"+e.getLocalizedMessage(),e);
		}
	}
}
