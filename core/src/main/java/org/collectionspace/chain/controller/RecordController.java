package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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

	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view");
		out.put("csid",csid);
		out.put("recordtype",ChainRequest.convertTypeToTypeURL(type));
		return out;		
	}
	
	private JSONObject generateEntry(Storage storage,String member) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		return generateMiniRecord(storage,base,member);
	}

	private JSONObject pathsToJSON(Storage storage,String[] paths,String key) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		JSONArray members=new JSONArray();
		for(String p : paths)
			members.put(generateEntry(storage,p));
		out.put(key,members);
		return out;
	}
	
	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject in=storage.retrieveJSON("relations/main/"+csid);
		String[] dstid=in.getString("dst").split("/");
		String type=in.getString("type");
		JSONObject mini=generateMiniRecord(storage,dstid[0],dstid[1]);
		mini.put("relationshiptype",type);
		mini.put("relid",in.getString("csid"));
		return mini;
	}
	
	private JSONArray createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONArray out=new JSONArray();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		String[] relations=storage.getPaths("relations/main",restrictions);
		for(String r : relations)
			out.put(generateRelationEntry(storage,r));
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

	// XXX we just assume name for now
	private String[] doAutocomplete(CSPRequestCache cache,String start) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		Storage storage=global.getStore().getStorage(cache);
		JSONObject restriction=new JSONObject();
		restriction.put("name",start);
		List<String> out=new ArrayList<String>();
		for(String urn : storage.getPaths("vocab/name",restriction)) {
			JSONObject data=storage.retrieveJSON("vocab/name/"+urn);
			JSONObject entry=new JSONObject();
			entry.put("urn",urn);
			entry.put("label",data.getString("name"));
			out.add(entry.toString());
		}
		return out.toArray(new String[0]);
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
		case SEARCH:
		case LIST:
			try {
				JSONObject restriction=new JSONObject();
				String key="items";
				if(request.getType()==RequestType.SEARCH) {
					restriction.put("keywords",request.getQueryParameter("query"));
					key="results";
				}
				String[] paths=storage.getPaths(base,restriction);
				for(int i=0;i<paths.length;i++) {
					if(paths[i].startsWith(base+"/"))
						paths[i]=paths[i].substring((base+"/").length());
				}
				out = request.getJSONWriter();
				out.write(pathsToJSON(storage,paths,key).toString());
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
		case AUTOCOMPLETE:			
			try {
				out = request.getPlainTextWriter();
				StringBuffer completions=new StringBuffer();
				for(String v : doAutocomplete(cache,request.getQueryParameter("q"))) {
					completions.append(v+"\n");
				}
				out.write(completions.toString());
			} catch (JSONException e) {
				throw new BadRequestException("Invalid JSON",e);
			} catch (ExistException e) {
				throw new BadRequestException("Existence problem",e);
			} catch (UnimplementedException e) {
				throw new BadRequestException("Unimplemented",e);
			} catch (UnderlyingStorageException e) {
				throw new BadRequestException("Problem storing",e);
			}
			break;
		}
		request.setStatus(HttpServletResponse.SC_OK);
	}

	private void deleteAllRelations(Storage storage,String csid) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject r=new JSONObject();
		r.put("src",base+"/"+csid);		
		for(String relation : storage.getPaths("relations/main", r)) {
			storage.deleteJSON("relations/main/"+relation);
		}
	}
	
	private void setRelations(Storage storage,String csid,JSONArray relations) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		deleteAllRelations(storage,csid);
		for(int i=0;i<relations.length();i++) {
			// Extract data from miniobject
			JSONObject in=relations.getJSONObject(i);
			String dst_type=ChainRequest.convertTypeURLToType(in.getString("recordtype"));
			String dst_id=in.getString("csid");
			String type=in.getString("relationshiptype");
			// Create relation
			JSONObject r=new JSONObject();
			r.put("src",base+"/"+csid);
			r.put("dst",dst_type+"/"+dst_id);
			r.put("type",type);
			storage.autocreateJSON("relations/main",r);
		}
	}
	
	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		JSONArray relations=data.optJSONArray("relations");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(base+"/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(base,fields);
		}
		if(relations!=null)
			setRelations(storage,path,relations);
		return path;
	}
	
	// XXX refactor
	private String getResource(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}
	
	private String xxx_mercury_search(String type,String value) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		CSPRequestCache cache=new RequestCache();
		Storage storage=global.getStore().getStorage(cache);
		String target_base=ChainRequest.convertTypeURLToType(type);
		for(String path : storage.getPaths(target_base,null)) {
			JSONObject mini=storage.retrieveJSON(target_base+"/"+path+"/view");
			if(mini==null)
				return null;
			System.err.println("record is "+mini);
			if(mini.has("number") && mini.getString("number").equals(value))
				return path;
			
		}
		return null;
	}
	
	private void xxx_mercury_associate(Storage storage,String source_id,String type,String value) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		String target_id=xxx_mercury_search(type,value);
		if(target_id==null)
			return;		
		System.err.println("target id is "+target_id);
		JSONObject data=new JSONObject();
		data.put("src",base+"/"+source_id);
		data.put("dst",ChainRequest.convertTypeURLToType(type)+"/"+target_id);
		data.put("type","affects");
		storage.autocreateJSON("relations/main",data);
	}
	
	private void xxx_mercury_related_records(Storage storage,String id) throws IOException, JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		System.err.println("Applying related record hack to "+id);		
		String assocs=getResource("mercury-relations.txt");
		for(String line : assocs.split("\n")) {
			String[] data=line.split(" ");
			if(data.length<3)
				continue;
			if(data[0].equals(base))
				xxx_mercury_associate(storage,id,data[1],data[2]);
		}
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
			if(request.isCreateNotOverwrite()) {
				path=sendJSON(storage,null,data);
				xxx_mercury_related_records(storage,path);
			} else
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
