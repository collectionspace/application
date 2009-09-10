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
public class SplittingStorage implements Storage {
	private Map<String,Storage> children=new HashMap<String,Storage>();
	
	public void addChild(String prefix,Storage store) {
		children.put(prefix,store);
	}
	
	private Storage get(String path) throws ExistException {
		Storage out=children.get(path);
		if(out==null)
			throw new ExistException("No child storage bound to "+path);
		return out;
	}
	
	private String[] split(String path,boolean missing_is_blank) throws ExistException {
		String[] out=path.split("/",2);
		if(out.length<2) {
			if(missing_is_blank)
				return new String[]{path,""};
			else
				throw new ExistException("Path is split point, not destination");
		}
		return out;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		if("".equals(parts[1])) { // autocreate?
			get(parts[0]).autocreateJSON("",jsonObject);
		}
		get(parts[0]).createJSON(parts[1],jsonObject);
	}

	public String[] getPaths(String rootPath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(rootPath,true);
		List<String> out=new ArrayList<String>();
		for(Map.Entry<String,Storage> e : children.entrySet()) {
			if(e.getKey().equals(parts[0]))
			for(String s : e.getValue().getPaths(parts[1])) {
				out.add(s);
			}
		}
		return out.toArray(new String[0]);
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		return get(parts[0]).retrieveJSON(parts[1]);
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).updateJSON(parts[1],jsonObject);
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		return get(parts[0]).autocreateJSON(parts[1],jsonObject);
	}

	public void deleteJSON(String filePath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).deleteJSON(parts[1]);		
	}
}
