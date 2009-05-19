package org.collectionspace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class HandleJSON extends HttpServlet 
{

	final static String SCHEMA_REF = "/schema/";
	final static String STORE_REF = "/store/object/";
	final static String ABSOLUTE_PATH = "/";

	final static String JSON_KEY_VALUE_SEPARATOR = ":";

	final static String JSON_TYPE_STRING = "string";
	final static String JSON_TYPE_BOOLEAN = "boolean";
	final static String JSON_TYPE_NUMBER = "number";
	final static String JSON_TYPE_JSONObject = "object";
	final static String JSON_TYPE_JSONArray = "array";
	final static String JSON_TYPE_NULL = "null";

	/**
	 * Responding to a request. The request is assumed to consist of a path to a requested JSON object.
	 * The response returns the object in string form (or an empty string if not found).
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// The input should just be a string identifying the path to the file
		String path = req.getPathInfo();
		if (path == null || path.length() == 0 || 	path.indexOf(SCHEMA_REF) < 0)
		{
			notifyUsage(resp);
			return;
		}

		// Set response type to JSON
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");

		String outputJSON;
		// Get the local path, either from the SCHEMA_REF or from the STORE_REF, depending on which one
		// is present
		int pos = path.indexOf(SCHEMA_REF); 
		if (pos >= 0)
		{
			int from = pos + SCHEMA_REF.length();
			if (from < path.length())
			{
				path = path.substring(from);
			}
			else
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file path supplied after " + SCHEMA_REF);
				return;
			}
		}
		else
		{
			pos = path.indexOf(STORE_REF);
			if (pos >= 0)
			{
				// get the path after store (should be the file path not including leading /)
				int from = pos + STORE_REF.length();
				if (from < path.length())
				{
					path = path.substring(from);
				}
				else
				{
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file path supplied after " + STORE_REF);
					return;
				}
			}
		}

		try
		{
			outputJSON = retrieveJson(ABSOLUTE_PATH + path);
		}
		catch (NotFoundException nfe)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "File not found: " + path);
			return;
		}

		// If there is no output something must have gone wrong
		if (outputJSON == null || outputJSON.length() == 0)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JSON found");
			return;			
		}

		// Write the requested JSON out
		PrintWriter out = resp.getWriter();
		try
		{
			out.write(outputJSON);
			out.close();
			resp.setStatus(HttpServletResponse.SC_OK);
		}
		catch (Exception onfe)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data " + path + " not found");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		doPut(req, resp);
	}

	/**
	 * Stores incoming JSON in a given location in the local storage. The location is derived from the incoming
	 * URL as the part following "/store/object/".
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		// The input should just be a string identifying the path to the file
		String jsonString;
		String path = req.getPathInfo();
		if (path == null || path.length() == 0 || path.indexOf(STORE_REF) < 0)
		{
			// get the path and json from params
			path = req.getParameter("storage");
			jsonString = req.getParameter("json_str");
		}
		else
		{
			// get the json from the body
			StringBuilder sb = new StringBuilder();
			BufferedReader br = req.getReader();
			String line = br.readLine();
			while (line != null)
			{
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			jsonString = sb.toString();
		}

		if (path != null && path.length() > 0 && path.indexOf(STORE_REF) >= 0)
		{
			int from = path.indexOf(STORE_REF) + STORE_REF.length();
			if (path.length() > from)
			{
				path = path.substring(from);
			}
		}
		else
		{
			notifyUsage(resp);
			return;
		}

		// Set response type to text/html
		resp.setContentType("text/html");

		if (jsonString == null || jsonString.length() == 0)
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JSON content to store");
			return; // END
		}
		else
		{
			// Get the actual JSON string and store it
			try
			{
				storeJson(ABSOLUTE_PATH + path, new JSONObject(jsonString));
				resp.addHeader("Location", STORE_REF + path);
				resp.setStatus(201);
				return; // END
			}
			catch (JSONException je)
			{
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse json: " + je);
			}
		}
	}

	private void notifyUsage(HttpServletResponse resp) throws IOException
	{
		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "You must structure the requests like so: \n" +
				"GET /schema/%path-to-file-with-name% \n" +
				"GET /store/object/%path-to-file-with-name% \n" +
				"POST /store/object/%path-to-file-with-name% - note that data in body must be JSON \n");
	}
	
	/**
	 * Gets a handle on a local file, provided its path contains the key term "/schema/". The variable
	 * part of the path to the actual file in the local store is the bit following "/schema/", and it is
	 * is appended to an absolute path to the top level of the store.
	 *  
	 * @param filePath - path to parse
	 * @return  a File object containing the referenced file, or null if not found
	 */
	private File getFileHandle(String filePath)
	{
		File result = null;
		if (filePath.indexOf(SCHEMA_REF) >= 0)
		{
			int from = filePath.indexOf(SCHEMA_REF) + SCHEMA_REF.length();
			if (filePath.length() > from)
			{
				String relativePath = filePath.substring(from);
				result = new File(ABSOLUTE_PATH + relativePath);
			}
		}
		return result;
	}

	/**
	 * Generates JSON string from a file storing the information
	 * 
	 * @param filePath - path to the file
	 * @return  String of valid JSON format, or an empty string if an error was encountered.
	 */
	protected String retrieveJson(String filePath)
	{
		JSONObject jsonObject = new JSONObject();
		File jsonFile = new File(filePath);
		if (!jsonFile.exists())
		{
			throw new NotFoundException("No such file: " + filePath);
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

	/**
	 * Parses and stores the given JSONObject in a file in the given path.
	 * 
	 * @param filePath - path to file for storage
	 * @param jsonObject - the JSONObject to be parsed and stored
	 */
	@SuppressWarnings("unchecked")
	protected void storeJson(String filePath, JSONObject jsonObject)
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

	public static class NotFoundException extends RuntimeException {
		public NotFoundException(String message, Throwable cause) {
			super(message, cause);
		}
		public NotFoundException(String message) {
			super(message);
		}
	}

}

