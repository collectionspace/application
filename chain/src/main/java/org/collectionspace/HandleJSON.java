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

	private final static String SCHEMA_REF = "/schema/";
	private final static String STORE_REF = "/store/object/";

	final static String ABSOLUTE_PATH = "/";
	
	JSONStore store=new StubJSONStore();
	
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
			outputJSON = store.retrieveJson(ABSOLUTE_PATH + path);
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
				store.storeJson(ABSOLUTE_PATH + path, new JSONObject(jsonString));
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
	
	public static class NotFoundException extends RuntimeException {
		public NotFoundException(String message, Throwable cause) {
			super(message, cause);
		}
		public NotFoundException(String message) {
			super(message);
		}
	}

}

