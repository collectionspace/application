package org.collectionspace.chain.jsonstore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StubJSONStore implements JSONStore {
	private final static String JSON_TYPE_STRING = "string";
	private final static String JSON_TYPE_BOOLEAN = "boolean";
	private final static String JSON_TYPE_NUMBER = "number";
	private final static String JSON_TYPE_JSONObject = "object";
	private final static String JSON_TYPE_JSONArray = "array";
	private final static String JSON_TYPE_NULL = "null";
		
	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#retrieveJson(java.lang.String)
	 */
	public String retrieveJson(String filePath) throws JSONNotFoundException {
		JSONObject jsonObject = new JSONObject();
		File jsonFile = new File(filePath);
		if (!jsonFile.exists())
		{
			throw new JSONNotFoundException("No such file: " + filePath);
		}

		// Put together a JSON object form the file. The file is assumed to contain one entry per line, where
		// an entry consists of the triplet type-key-value, separated by spaces.
		try {
			BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
			String line = reader.readLine();
			while (line != null)
			{
				String type = "";
				String key = "";
				String value = "";

				// Ignore lines that do not follow the precise "type key value" format
				// First parameter is type
				int pos = line.indexOf(" ");
				if (pos > 0)
				{
					type = line.substring(0,pos).trim();
					line = line.substring(pos).trim();
					// Second parameter is key
					pos = line.indexOf(" ");
					if (pos > 0)
					{
						key = line.substring(0,pos).trim();
						// Third parameter is value, made up of the rest of the line
						value = line.substring(pos).trim();
					}
					else
					{
						return "";
					}
				}
				else
				{
					return "";
				}

				if (type.equals(JSON_TYPE_BOOLEAN))
				{
					jsonObject.put(key, new Boolean(value));
				}
				// Numbers are put in as Doubles
				else if (type.equals(JSON_TYPE_NUMBER))
				{
					jsonObject.put(key, new Double(value));
				}
				else if (type.equals(JSON_TYPE_STRING))
				{
					jsonObject.put(key, value);
				}
				else if (type.equals(JSON_TYPE_JSONArray))
				{
					jsonObject.put(key, new JSONArray(value));
				}
				else if (type.equals(JSON_TYPE_JSONObject))
				{
					jsonObject.put(key, new JSONObject(value));	
				}
				else if (type.equals(JSON_TYPE_NULL))
				{
					jsonObject.put(key, JSONObject.NULL);
				}

				// Get the next line from the file
				line = reader.readLine();
			}
			return jsonObject.toString();
		}
		catch (IOException ioe)
		{
			return "";
		}
		catch (JSONException je)
		{
			return "";			
		}
	}

	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#storeJson(java.lang.String, org.json.JSONObject)
	 */
	@SuppressWarnings("unchecked")
	public void storeJson(String filePath, JSONObject jsonObject)
	{
		System.out.println("file path:" + filePath);
		File jsonFile = new File(filePath);

		System.out.println("storing json");
		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));

			// Parse the JSON string into a file where each line is of the form "type key value".
			for (Iterator iter = jsonObject.keys(); iter.hasNext(); )
			{
				String key = (String)iter.next();
				Object value = jsonObject.get(key);
				String type = "";
				if (value == null)
				{
					type = JSON_TYPE_NULL;
				}
				else if (value instanceof Boolean)
				{
					type = JSON_TYPE_BOOLEAN;
				}
				else if (value instanceof Integer ||
						value instanceof Float ||
						value instanceof Double)
				{
					type = JSON_TYPE_NUMBER;
				}
				else if (value instanceof String)
				{
					type = JSON_TYPE_STRING;
				}
				else if (value instanceof JSONArray)
				{
					type = JSON_TYPE_JSONArray;
				}
				else if (value instanceof JSONObject)
				{
					type = JSON_TYPE_JSONObject;
				}
				// Only write out if there is a type
				if (type != "")
				{
					writer.write(type + " " + key + " " + value + "\n");
				}
			}
			writer.flush();
			writer.close();
		}
		catch (JSONException je)
		{
			return;
		}
		catch (IOException ioe)
		{
			return;
		}
	}
}
