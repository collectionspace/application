/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */

package org.collectionspace.chain.util.jpath;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** JPath is a simple way of specifying a path in JSON, reasonably analagous to XPath.
 * 
 * A path is composed of a number of components.
 * Each component begins with a "." (dot type) or a "[" (bracket type), including the first component.
 * 
 * A bracket type ends with the first ]. Inside these brackets are an a string or a number.
 * 
 * A dot type is terminated by the end of path or next component part (. or [). Between them may be any unicode
 * letter, $, _, or unicode escape sequence to start, and then any of the above, or digit, or anything in unicode Pc.
 * 
 * @author dan
 *
 */

public class JPathPath {
	private String path_string;
	private List<Object> path=new ArrayList<Object>();
	
	public static JPathPath compile(String path) throws InvalidJPathException {
		return new JPathPath(path);
	}

	private void compile_path() throws InvalidJPathException {
		path=new JPathPathParser(path_string).getResult();
	}
	
	private JPathPath(String path) throws InvalidJPathException {
		path_string=path;
		compile_path();
	}
	
	List<Object> getPath() { return path; }
	
	// src and ret are one of json types, part is an element in our path 
	Object step(Object src,Object part) throws JSONException {
		if(src instanceof JSONObject) {
			if(part instanceof String)
				return ((JSONObject) src).opt((String)part);
			return null;
		}
		if(src instanceof JSONArray) {
			if(part instanceof Integer)
				return ((JSONArray)src).opt(((Integer) part).intValue());
			return null;
		}
		return null;
	}

	// src and ret are one of json types, part is an element in our path 
	void set_step(Object src,Object part,Object value) throws JSONException {
		if(src instanceof JSONObject) {
			if(part instanceof String)
				((JSONObject) src).put((String)part,value);
			return;
		}
		if(src instanceof JSONArray) {
			if(part instanceof Integer)
				((JSONArray)src).put(((Integer) part).intValue(),value);
			return;
		}
	}
	
	public Object get(Object src) throws JSONException {
		Object cur=src;
		for(Object step : path)
			cur=step(cur,step);
		return cur;
	}
	
	public void set(Object src,Object value) throws JSONException {
		Object cur=src;
		for(int i=0;i<path.size();i++) {
			if(i<path.size()-1) {
				cur=step(cur,path.get(i));
			} else {
				set_step(cur,path.get(i),value);
			}
		}
	}
	
	public String getString(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof String) && out!=null)
			throw new InvalidJPathException("Not a string");
		return (String)out;
	}

	public boolean getBoolean(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(out==null)
			return false;
		if(!(out instanceof Boolean))
			throw new InvalidJPathException("Not a boolean");
		return ((Boolean)out).booleanValue();
	}

	public boolean isNull(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		return out!=null;
	}
	
	public JSONObject getJSONObject(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof JSONObject) && out!=null)
			throw new InvalidJPathException("Not a JSONObject");
		return (JSONObject)out;
	}

	public JSONArray getJSONArray(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof JSONArray) && out!=null)
			throw new InvalidJPathException("Not a JSONArray");
		return (JSONArray)out;
	}
	
	public static String escape(String in) {
		return StringEscapeUtils.escapeJavaScript(in);
	}
	
	public static String component(String in) {
		return "[\""+escape(in)+"\"]";
	}
}
