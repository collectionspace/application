package org.collectionspace;

import org.json.JSONObject;

public interface JSONStore {

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	public abstract String retrieveJson(String filePath);

	/**
	 * Parses and stores the given JSONObject in a file in the given path.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void storeJson(String filePath, JSONObject jsonObject);

}