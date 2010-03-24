/* Copyright 2009 University of Cambridge
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
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
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

	private String[] split(String path,boolean missing_is_blank) throws ExistException {
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

	public void createJSON(ContextualisedStorage root,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		if("".equals(parts[1])) { // autocreate?
			get(parts[0]).autocreateJSON(root,cache,"",jsonObject);
			return;
		}
		get(parts[0]).createJSON(root,cache,parts[1],jsonObject);
	}

	public String[] getPaths(ContextualisedStorage root,CSPRequestCache cache,String rootPath,JSONObject restriction) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(rootPath,true);
		if("".equals(parts[0])) {
			return children.keySet().toArray(new String[0]);
		} else {
			List<String> out=new ArrayList<String>();
			for(Map.Entry<String,ContextualisedStorage> e : children.entrySet()) {
				if(e.getKey().equals(parts[0])) {
					ContextualisedStorage storage=e.getValue();
					String[] paths=storage.getPaths(root,cache,parts[1],restriction);
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

	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCache cache,String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		return get(parts[0]).retrieveJSON(root,cache,parts[1]);
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).updateJSON(root,cache,parts[1],jsonObject);
	}

	public String autocreateJSON(ContextualisedStorage root,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		return get(parts[0]).autocreateJSON(root,cache,parts[1],jsonObject);
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).deleteJSON(root,cache,parts[1]);		
	}
}
