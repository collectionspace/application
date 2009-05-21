package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.jsonstore.JSONNotFoundException;
import org.collectionspace.chain.jsonstore.JSONStore;
import org.collectionspace.chain.jsonstore.StubJSONStore;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class ChainServlet extends HttpServlet 
{
	private final static String STORE_REF = "/store/object/";

	public final static String ABSOLUTE_PATH = "/";

	JSONStore store=new StubJSONStore();

	private String getJSON(String path) throws BadRequestException {
		String out;
		try {
			out = store.retrieveJson(ABSOLUTE_PATH + path);
		} catch (JSONNotFoundException e) {
			throw new BadRequestException("JSON Not found "+e,e);
		}
		if (out == null || "".equals(out)) {
			throw new BadRequestException("No JSON Found");
		}
		return out;
	}
	
	private boolean perhapsServeFixedContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(servlet_request.getPathInfo());
		if(is==null)
			return false; // Not for us
		// Serve fixed content
		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
	
	/**
	 * Responding to a request. The request is assumed to consist of a path to a requested JSON object.
	 * The response returns the object in string form (or an empty string if not found).
	 */
	@Override
	public void doGet(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException
	{
		if(perhapsServeFixedContent(servlet_request,servlet_response))
			return;
		try {
			// Setup our request object
			ChainRequest request=new ChainRequest(servlet_request,servlet_response,true);
			
			// Get the data
			String outputJSON = getJSON(request.getPathTail());

			// Write the requested JSON out
			PrintWriter out = request.getJSONWriter();
			out.write(outputJSON);
			out.close();
			servlet_response.setStatus(HttpServletResponse.SC_OK);

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
	public void doPut(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException
	{
		String path=null;
		String jsonString=null;
		// Setup our request object
		try {
			// Get various bits out of the request
			ChainRequest request=new ChainRequest(servlet_request,servlet_response,false);
			path = request.getPathTail();
			jsonString = request.getBody();
			if (StringUtils.isBlank(jsonString)) {
				throw new BadRequestException("No JSON content to store");
			}
			// Store it
			try {
				store.storeJson(ABSOLUTE_PATH + path, new JSONObject(jsonString));
			} catch (JSONException x) {
				throw new BadRequestException("Failed to parse json: "+x,x);
			}
			// Created!
			servlet_response.setContentType("text/html");
			servlet_response.addHeader("Location", STORE_REF + path);
			servlet_response.setStatus(201);
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());			
		}
	}
}
