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
	private static final Pattern id_pattern=Pattern.compile("\\*\\*\\*(.*)\\*\\*\\*");
	private static final Random rnd=new Random();
	private static final char[] letters=new char[]{'a','b','c','d','e','f','g','h','i','j',
		'k','l','m','n','o','p','q','r','s','t',
		'u','v','w','x','y','z'};
	private static final char[] digits=new char[]{'0','1','2','3','4','5','6','7','8','9'};

	RecordController(ControllerGlobal global,String base) {
		this.base=base;
		this.global=global;
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

	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String path) throws BadRequestException {
		JSONObject out;
		try {
			out = storage.retrieveJSON(path);
		} catch (ExistException e) {
			throw new BadRequestException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new BadRequestException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new BadRequestException("Problem getting",e);
		}
		if (out == null) {
			throw new BadRequestException("No JSON Found");
		}
		return out;
	}

	private String xxx_replace_number(Storage storage,String in) throws BadRequestException, JSONException {
		Matcher m=id_pattern.matcher(in);
		if(!m.matches())
			return in;
		JSONObject seq=getJSON(storage,"id/"+m.group(1));
		return (String)seq.getString("next");
	}

	private JSONObject replaceNumbers(Storage storage,JSONObject in) throws JSONException, BadRequestException {
		JSONObject out=new JSONObject();
		Iterator<?> keys=in.keys();
		while(keys.hasNext()) {
			String k=(String)keys.next();
			String v=in.getString(k);
			out.put(k,xxx_replace_number(storage,v));
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
			JSONObject outputJSON = getJSON(storage,base+"/"+path);
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

	public void send(ChainRequest request,String path) throws BadRequestException, IOException {
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		String jsonString = request.getBody();
		if (StringUtils.isBlank(jsonString)) {
			throw new BadRequestException("No JSON content to store");
		}
		// Store it
		int status=200;
		try {
			JSONObject data=new JSONObject(jsonString);
			if(request.isCreateNotOverwrite()) {
				if("".equals(path)) {
					// True path
					path=storage.autocreateJSON(base,data);
					data.put("csid",path);
				} else {
					// XXX temporary legacy path
					storage.createJSON(base+"/"+path,data);
				}
				status=201;
			} else
				storage.updateJSON(base+"/"+path,data);
			request.getJSONWriter().print(data.toString());
			request.setContentType("text/html");
			request.setStatus(status);
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
