package org.collectionspace.chain.util.jpath;

import java.util.ArrayList;
import java.util.List;

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
	
	public Object get(Object src) throws JSONException {
		Object cur=src;
		for(Object step : path)
			cur=step(cur,step);
		return cur;
	}
	
	public String getString(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof String))
			throw new InvalidJPathException("Not a string");
		return (String)out;
	}

	public boolean getBoolean(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof Boolean))
			throw new InvalidJPathException("Not a boolean");
		return ((Boolean)out).booleanValue();
	}
	
	public JSONObject getJSONObject(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof JSONObject))
			throw new InvalidJPathException("Not a JSONObject");
		return (JSONObject)out;
	}

	public JSONArray getJSONArray(Object src) throws InvalidJPathException, JSONException {
		Object out=get(src);
		if(!(out instanceof JSONArray))
			throw new InvalidJPathException("Not a JSONArray");
		return (JSONArray)out;
	}
}
