/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.persistence;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONObject;

/** The core SVCAPP interface.
 * 
 */
public interface ContextualisedStorage {

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	public abstract JSONObject retrieveJSON(CSPRequestCache cache,String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it already exists.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void updateJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it does not exist.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void createJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	public abstract String autocreateJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public String[] getPaths(CSPRequestCache cache,String rootPath)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public void deleteJSON(CSPRequestCache cache,String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
}