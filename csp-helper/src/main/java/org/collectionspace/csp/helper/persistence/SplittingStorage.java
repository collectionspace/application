/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;

/** SplittingStorage is an implementation of storage which can be wrapped or used as a base class, which delegates 
 * the execution of methods to another implementation of storage on the basis of the first path component. This 
 * allows different code to execute in different path subtrees.
 * 
 */
public class SplittingStorage implements ContextualisedStorage {
	private Map<String,ContextualisedStorage> children=new HashMap<String,ContextualisedStorage>();

	
	public void addChild(String prefix,ContextualisedStorage store) {
		children.put(prefix,store);
	}

	private ContextualisedStorage get(String path) throws ExistException {
		ContextualisedStorage out=children.get(path);
		if(out==null)
			throw new ExistException("No child storage bound to "+path);
		return out;
	}

	private String[] split(String path,boolean missing_is_blank) throws ExistException { // REM - Please document so I don't have to interpret this parsing code each time.
		if(path.startsWith("/"))
			path=path.substring(1);
		String[] out=path.split("/",2);
		if(out.length<2) {
			if(missing_is_blank)
				return new String[]{path,""};
			else
				throw new ExistException("Path is split point, not destination");
		}
		return out;
	}

	@Override
	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		if("".equals(parts[1])) { // autocreate?
			get(parts[0]).autocreateJSON(root,creds,cache,"",jsonObject, null /*no path restrictions*/);
			return;
		}
		get(parts[0]).createJSON(root,creds,cache,parts[1],jsonObject);
	}

	/**
	 * For each type of storage, this function will get the paths and pagination information, this will be brought together into one object
	 */
	@Override
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restriction) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try{
			
			//XXX THIS SHOULD BE LOOKED AT AND CHANGED !!!
			JSONObject out = new JSONObject();
			JSONObject pagination = new JSONObject();
			JSONObject moredata = new JSONObject();
			boolean passed = false;
			List<String[]> separatelists = new ArrayList<String[]>();
			String parts[]=split(rootPath,true);
			if("".equals(parts[0])) {
				return out.put("listItems",children.keySet().toArray(new String[0]));
			} else {
				List<String> list=new ArrayList<String>();
				for(Map.Entry<String,ContextualisedStorage> e : children.entrySet()) {
					if(e.getKey().equals(parts[0])) {
						ContextualisedStorage storage=e.getValue();
						JSONObject data=storage.getPathsJSON(root,creds,cache,parts[1],restriction);
						if(data==null){
							data = new JSONObject();
							data.put("listItems", new String[0]) ;
						}
						if(data.has("moredata")){
							moredata = data.getJSONObject("moredata");
						}
						JSONObject paging = new JSONObject();
						if(data.has("pagination")){
							paging = data.getJSONObject("pagination");
						}
						if(!passed){
							pagination = paging;
							passed = true;
						}else{
							pagination.put("totalItems",pagination.getInt("totalItems") + paging.getInt("totalItems"));
							int pageSize = pagination.getInt("pageSize");
							int totalinpage = pagination.getInt("itemsInPage") + paging.getInt("itemsInPage");
							if(totalinpage > pageSize){
								pagination.put("itemsInPage",pageSize);
							}else{
								pagination.put("itemsInPage", totalinpage);
							}
						}

						//create one merged list
						String[] paths = (String[]) data.get("listItems");
						if(paths==null){
							continue;
						}
						for(String s : paths) {
							list.add(s);
						}

						//keep the separate lists in a field
						separatelists.add(paths);
					}
				}
				
				pagination.put("separatelists", separatelists);
				out.put("moredata", moredata);
				out.put("pagination", pagination);
				out.put("listItems",list.toArray(new String[0]));
				
				return out;
			}
		}catch(JSONException e){
			throw new UnderlyingStorageException("Error parsing JSON");
		}
	}
	
	@Override
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restriction) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(rootPath,true);
		if("".equals(parts[0])) {
			return children.keySet().toArray(new String[0]);
		} else {
			List<String> out=new ArrayList<String>();
			for(Map.Entry<String,ContextualisedStorage> e : children.entrySet()) {
				if(e.getKey().equals(parts[0])) {
					ContextualisedStorage storage=e.getValue();
					String[] paths=storage.getPaths(root,creds,cache,parts[1],restriction);
					if(paths==null)
						continue;
					for(String s : paths) {
						out.add(s);
					}
				}
			}
			return out.toArray(new String[0]);
		}
	}

	@Override
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		return get(parts[0]).retrieveJSON(root,creds,cache,parts[1],restrictions);
	}

	@Override
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).updateJSON(root,creds,cache,parts[1],jsonObject, restrictions);
	}

	@Override
	public String autocreateJSON(
			ContextualisedStorage root,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String filePath, 
			JSONObject jsonObject, 
			JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true); // REM 
		return get(parts[0]).autocreateJSON(root,creds,cache,parts[1],jsonObject, restrictions);
	}

	@Override
	public void deleteJSON(
			ContextualisedStorage root,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String filePath) throws ExistException,	UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).deleteJSON(root,creds,cache,parts[1]);		
	}

	@Override
	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, 
			String filePath, String workflowTransition) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).transitionWorkflowJSON(root,creds,cache,parts[1],workflowTransition);
	}
	
}
