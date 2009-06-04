package org.collectionspace.chain.storage;

import org.json.JSONObject;

public interface Storage {

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	public abstract String retrieveJSON(String filePath) throws NotExistException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it already exists.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void updateJSON(String filePath, JSONObject jsonObject) throws ExistException;

	/**
	 * Parses and stores the given JSONObject in a file in the given path, if it does not exist.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	public abstract void createJSON(String filePath, JSONObject jsonObject) throws ExistException;
	
	public String[] getPaths();
}