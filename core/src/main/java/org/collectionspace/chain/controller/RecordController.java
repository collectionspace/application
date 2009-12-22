package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.core.RequestCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RecordController {
	private String base;
	private ControllerGlobal global;
	private boolean record;
	
	RecordController(ControllerGlobal global,String base,boolean record) {
		this.base=base;
		this.global=global;
		this.record=record;
	}

	private JSONObject generateEntry(Storage storage,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=storage.retrieveJSON(base+"/"+member+"/view");
		out.put("csid",member);
		return out;
	}

	private JSONObject pathsToJSON(Storage storage,String[] paths) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,p));
		out.put("items",members);
		return out;
	}

	private JSONArray createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONArray out=new JSONArray();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		String[] relations=storage.getPaths("relations/main",restrictions);
		for(String r : relations)
			out.put(r);
		return out;
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws BadRequestException {
		JSONObject out=new JSONObject();
		try {
			if(record) {
				JSONObject fields=storage.retrieveJSON(base+"/"+csid);
				fields.put("csid",csid); // XXX remove this, subject to UI team approval?
				JSONArray relations=createRelations(storage,csid);
				out.put("fields",fields);
				out.put("relations",relations);
			} else {
				out=storage.retrieveJSON(base+"/"+csid);
			}
		} catch (ExistException e) {
			throw new BadRequestException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new BadRequestException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new BadRequestException("Problem getting",e);
		} catch (JSONException e) {
			throw new BadRequestException("Could not create JSON"+e,e);
		}
		if (out == null) {
			throw new BadRequestException("No JSON Found");
		}
		return out;
	}

	void doGet(ChainRequest request,String path) throws BadRequestException, IOException {
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		PrintWriter out;
		switch(request.getType()) {
		case STORE:
			// Get the data
			JSONObject outputJSON = getJSON(storage,path);
			try {
				outputJSON.put("csid",path);
			} catch (JSONException e1) {
				throw new BadRequestException("Cannot add csid",e1);
			}
			// Write the requested JSON out
			out = request.getJSONWriter();
			out.write(outputJSON.toString());
			out.close();
			break;
		case AUTO:
			// Get the data
			outputJSON = new JSONObject(); // XXX implement __auto properly
			// Write the requested JSON out
			out = request.getJSONWriter();
			out.write(outputJSON.toString());
			out.close();
			break;
		case SCHEMA:
			try {
				String data = global.getSchema().getSchema(base+"/"+path).toString();
				out = request.getJSONWriter();
				out.write(data);
				out.close();
			} catch (JSONException e) {
				throw new BadRequestException("Invalid JSON");
			} catch(IOException e) {
				throw new BadRequestException("Not found"); // XXX should be 404
			}
			break;
		case LIST:
			try {
				String[] paths=storage.getPaths(base,null);
				for(int i=0;i<paths.length;i++) {
					if(paths[i].startsWith(base+"/"))
						paths[i]=paths[i].substring((base+"/").length());
				}
				out = request.getJSONWriter();
				out.write(pathsToJSON(storage,paths).toString());
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
			break;
		}
		request.setStatus(HttpServletResponse.SC_OK);
	}

	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(base,fields);
		}
		return path;
	}
	
	public void send(ChainRequest request,String path) throws BadRequestException, IOException {
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		String jsonString = request.getBody();
		if (StringUtils.isBlank(jsonString)) {
			throw new BadRequestException("No JSON content to store");
		}
		// Store it
		try {
			JSONObject data=new JSONObject(jsonString);
			if(request.isCreateNotOverwrite())
				path=sendJSON(storage,null,data);
			else
				path=sendJSON(storage,path,data);
			if(path==null)
				throw new BadRequestException("Insufficient data for create (no fields?)");
			data.put("csid",path);
			request.getJSONWriter().print(data.toString());
			request.setContentType("text/html");
			request.setStatus(request.isCreateNotOverwrite()?201:200);
			request.redirect("/"+request.getRecordTypeURL()+"/"+path);
		} catch (JSONException x) {
			throw new BadRequestException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			throw new BadRequestException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			throw new BadRequestException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new BadRequestException("Problem storing: "+x,x);
		}
	}

	public void doDelete(ChainRequest request,String path) throws BadRequestException {
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		try {
			storage.deleteJSON(base+"/"+path);
		} catch (ExistException x) {
			throw new BadRequestException("Existence exception: "+x,x); // XXX 404, not existence exception
		} catch (UnimplementedException x) {
			throw new BadRequestException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new BadRequestException("Problem storing: "+x,x);
		}
		request.setContentType("text/plain");
		request.setStatus(HttpServletResponse.SC_OK);
	}
}
