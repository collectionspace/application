/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.persistence;

import org.json.JSONObject;

/** The core SVCAPP interface.
 * 
 */
public interface Storage {

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	public JSONObject retrieveJSON(String filePath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it already exists.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public void updateJSON(String filePath, JSONObject jsonObject, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it does not exist.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public void createJSON(String filePath, JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException;

	public String autocreateJSON(String filePath, JSONObject jsonObject, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public String[] getPaths(String rootPath,JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public JSONObject getPathsJSON(String rootPath,JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public void deleteJSON(String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException;
	
	public void transitionWorkflowJSON(String filePath, String workflowTransition) 
		throws ExistException, UnimplementedException, UnderlyingStorageException;
}