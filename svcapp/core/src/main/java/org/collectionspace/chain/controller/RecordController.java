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
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
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

	private JSONObject pathsToJSON(String[] paths) throws JSONException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(p);
		out.put("items",members);
		return out;
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(String path) throws BadRequestException {
		JSONObject out;
		try {
			out = global.getStore().retrieveJSON(path);
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

	private String xxx_replace_number(String in) {
		Matcher m=id_pattern.matcher(in);
		if(!m.matches())
			return in;
		StringBuffer out=new StringBuffer();
		for(int i=0;i<m.group(1).length();i++) {
			char c=m.group(1).charAt(i);
			switch(c) {
			case '@':
				c=letters[rnd.nextInt(letters.length)];
				break;
			case '%':
				c=digits[rnd.nextInt(digits.length)];
				break;
			default:
				break;
			}
			out.append(c);
		}
		return out.toString();
	}
	
	private JSONObject replaceNumbers(JSONObject in) throws JSONException {
		JSONObject out=new JSONObject();
		Iterator<?> keys=in.keys();
		while(keys.hasNext()) {
			String k=(String)keys.next();
			String v=in.getString(k);
			out.put(k,xxx_replace_number(v));
		}
		return out;
	}
	
	void doGet(ChainRequest request,String path) throws BadRequestException, IOException {
		PrintWriter out;
		switch(request.getType()) {
		case STORE:
			// Get the data
			JSONObject outputJSON = getJSON(base+"/"+path);
			// Write the requested JSON out
			out = request.getJSONWriter();
			out.write(outputJSON.toString());
			out.close();
			break;
		case AUTO:
			try {
				// Get the data
				outputJSON = getJSON(base+"/__auto");
				// Update numbers
						outputJSON = replaceNumbers(outputJSON);
				// Write the requested JSON out
				out = request.getJSONWriter();
				out.write(outputJSON.toString());
				out.close();
			} catch (JSONException e) {
				throw new BadRequestException("Invalid JSON",e);
			}
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
				String[] paths=global.getStore().getPaths(base);
				for(int i=0;i<paths.length;i++) {
					if(paths[i].startsWith(base+"/"))
						paths[i]=paths[i].substring((base+"/").length());
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
			break;
		}
		request.setStatus(HttpServletResponse.SC_OK);
	}
	
	public void send(ChainRequest request,String path) throws BadRequestException, IOException {
		if(request.getType()==RequestType.AUTO)
			path="__auto"; // XXX Posting to __auto should be disabled. Currently used in some tests.
		String jsonString = request.getBody();
		if (StringUtils.isBlank(jsonString)) {
			throw new BadRequestException("No JSON content to store");
		}
		// Store it
		int status=200;
		try {
			if(request.isCreateNotOverwrite()) {
				global.getStore().createJSON(base+"/"+path,new JSONObject(jsonString));
				status=201;
			} else
				global.getStore().updateJSON(base+"/"+path, new JSONObject(jsonString));
			request.getJSONWriter().print(jsonString);
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
		try {
			global.getStore().deleteJSON(base+"/"+path);
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
