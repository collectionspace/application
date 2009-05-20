package org.collectionspace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	public void doGet(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException
	{

		// The input should just be a string identifying the path to the file
		String path = servlet_request.getPathInfo();
		if (path == null || path.length() == 0 )
		{
			notifyUsage(servlet_response);
			return;
		}

		// Set response type to JSON
		servlet_response.setCharacterEncoding("UTF-8");
		servlet_response.setContentType("application/json");

		String outputJSON;


		// Get the local path, either from the SCHEMA_REF or from the STORE_REF, depending on which one
		// is present
		try {
			ChainRequest request=new ChainRequest(servlet_request,servlet_response);
			path=request.getPathTail();
			outputJSON = store.retrieveJson(ABSOLUTE_PATH + path);

			// If there is no output something must have gone wrong
			if (outputJSON == null || outputJSON.length() == 0)
			{
				servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JSON found");
				return;			
			}

			// Write the requested JSON out
			PrintWriter out = servlet_response.getWriter();
			try
			{
				out.write(outputJSON);
				out.close();
				servlet_response.setStatus(HttpServletResponse.SC_OK);
			}
			catch (Exception onfe)
			{
				servlet_response.sendError(HttpServletResponse.SC_NOT_FOUND, "Data " + path + " not found");
			}

		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());
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
