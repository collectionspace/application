package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.schema.SchemaStore;
import org.collectionspace.chain.schema.StubSchemaStore;
import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.storage.StorageRegistry;
import org.collectionspace.chain.storage.UnderlyingStorageException;
import org.collectionspace.chain.storage.UnimplementedException;
import org.collectionspace.chain.storage.file.FileStorage;
import org.collectionspace.chain.storage.file.StubJSONStore;
import org.collectionspace.chain.storage.services.ServicesStorage;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.dom4j.DocumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class ChainServlet extends HttpServlet 
{	
	private Config config=null;
	private Storage store=null;
	private SchemaStore schema=null;
	private boolean inited=false;
	
	/* Not in the constructor because errors during construction of servlets tend to get lost in a mess of startup.
	 * Better present it on first request.
	 */
	
	private Storage getStorage() throws InvalidJXJException, DocumentException, IOException {
		// Complexity here is to allow for refactoring when storages can come from plugins
		StorageRegistry storage_registry=new StorageRegistry();
		storage_registry.addStorage("file",new FileStorage(config.getPathToSchemaDocs()));
		storage_registry.addStorage("service",new ServicesStorage(config.getServicesBaseURL()));
		return storage_registry.getStorage(config.getStorageType());
	}
	
	private synchronized void setup() throws BadRequestException {
		if(inited)
			return;
		try {
			config=new Config(getServletContext());
			store=getStorage();
		} catch (IOException e) {
			throw new BadRequestException("Cannot load config"+e,e);
		} catch (InvalidJXJException e) { // XXX better exception handling in ServicesStorage constructor
			throw new BadRequestException("Cannot load backend"+e,e);
		} catch (DocumentException e) {
			throw new BadRequestException("Cannot load backend"+e,e);
		}
		schema=new StubSchemaStore(config.getPathToSchemaDocs());
		inited=true;
	}
	
	private String getJSON(String path) throws BadRequestException {
		String out;
		try {
			out = store.retrieveJSON(path);
		} catch (ExistException e) {
			throw new BadRequestException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new BadRequestException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new BadRequestException("Problem storing",e);
		}
		if (out == null || "".equals(out)) {
			throw new BadRequestException("No JSON Found");
		}
		return out;
	}
	
	private boolean perhapsServeFixedContent(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		String pathinfo=servlet_request.getPathInfo();
		if(pathinfo.startsWith("/"))
			pathinfo=pathinfo.substring(1);
		InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(pathinfo);
		if(is==null)
			return false; // Not for us
		// Serve fixed content
		IOUtils.copy(is,servlet_response.getOutputStream());
		return true;
	}
	
	private JSONObject pathsToJSON(String[] paths) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(p);
		out.put("items",members);
		return out;
	}
	
	/**
	 * Responding to a request. The request is assumed to consist of a path to a requested JSON object.
	 * The response returns the object in string form (or an empty string if not found).
	 */
	@Override
	public void doGet(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(!inited)
				setup();
			if(perhapsServeFixedContent(servlet_request,servlet_response))
				return;
			// Setup our request object
			ChainRequest request=new ChainRequest(servlet_request,servlet_response);
			PrintWriter out;
			switch(request.getType()) {
			case STORE:
				// Get the data
				String outputJSON = getJSON("collection-object/"+request.getPathTail());

				// Write the requested JSON out
				out = request.getJSONWriter();
				out.write(outputJSON);
				out.close();
				servlet_response.setStatus(HttpServletResponse.SC_OK);
				break;
			case SCHEMA:
					try {
						String data = schema.getSchema(request.getPathTail()).toString();
						out = request.getJSONWriter();
						out.write(data);
						out.close();
						servlet_response.setStatus(HttpServletResponse.SC_OK);
					} catch (JSONException e) {
						throw new BadRequestException("Invalid JSON");
					} catch(IOException e) {
						throw new BadRequestException("Not found"); // XXX should be 404
					}
				break;
			case LIST:
				try {
					String[] paths=store.getPaths();
					for(int i=0;i<paths.length;i++) {
						if(paths[i].startsWith("collection-object/"))
							paths[i]=paths[i].substring("collection-object/".length());
					}
					out = request.getJSONWriter();
					out.write(pathsToJSON(paths).toString());
				} catch (JSONException e) {
					throw new BadRequestException("Invalid JSON",e);
				} catch (ExistException e) {
					throw new BadRequestException("Existence problem",e);
				} catch (UnimplementedException e) {
					throw new BadRequestException("Unimplemented",e);
				} catch (UnderlyingStorageException e) {
					throw new BadRequestException("Problem storing",e);
				}
				out.close();
				servlet_response.setStatus(HttpServletResponse.SC_OK);				
				break;
			}
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());
		}
	}

	@Override
	protected void doPost(HttpServletRequest servlet_request,HttpServletResponse servlet_response)
		throws ServletException, IOException {
		send(servlet_request,servlet_response);
	}

	/**
	 * Stores incoming JSON in a given location in the local storage. The location is derived from the incoming
	 * URL as the part following "/store/object/".
	 */
	@Override
	public void doPut(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		send(servlet_request,servlet_response);
	}

	private void send(HttpServletRequest servlet_request, HttpServletResponse servlet_response) throws ServletException, IOException {
		try {
			if(!inited)
				setup();
			// Get various bits out of the request
			ChainRequest request=new ChainRequest(servlet_request,servlet_response);
			String path = request.getPathTail();
			String jsonString = request.getBody();
			if (StringUtils.isBlank(jsonString)) {
				throw new BadRequestException("No JSON content to store");
			}
			// Store it
			int status=200;
			try {
				if(request.isCreateNotOverwrite()) {
					store.createJSON("collection-object/"+path,new JSONObject(jsonString));
					status=201;
				} else
					store.updateJSON("collection-object/"+path, new JSONObject(jsonString));
			} catch (JSONException x) {
				throw new BadRequestException("Failed to parse json: "+x,x);
			} catch (ExistException x) {
				throw new BadRequestException("Existence exception: "+x,x);
			} catch (UnimplementedException x) {
				throw new BadRequestException("Unimplemented exception: "+x,x);
			} catch (UnderlyingStorageException x) {
				throw new BadRequestException("Problem storing: "+x,x);
			}
			// Created!
			servlet_response.setContentType("text/html");
			servlet_response.addHeader("Location",request.getStoreURL(path));
			servlet_response.setStatus(status);
		} catch (BadRequestException x) {
			servlet_response.sendError(HttpServletResponse.SC_BAD_REQUEST, x.getMessage());			
		}
	}
}
