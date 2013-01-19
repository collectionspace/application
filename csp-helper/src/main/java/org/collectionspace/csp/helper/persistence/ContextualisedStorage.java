/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.persistence;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONObject;

/** The core SVCAPP interface.
 * 
 */
public interface ContextualisedStorage {
	public static final String WORKFLOW_TRANSITION_DELETE = "delete";
	public static final String WORKFLOW_SUBRESOURCE = "/workflow";

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	public abstract JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it already exists.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it does not exist.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	public abstract String autocreateJSON(
			ContextualisedStorage root,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String filePath,
			JSONObject jsonObject,
			JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public void deleteJSON(
			ContextualisedStorage root,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, 
							String filePath, String workflowTransition) 
		throws ExistException, UnimplementedException, UnderlyingStorageException;
}