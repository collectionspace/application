/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Some useful utilities to help us with org.json.
 * 
 */
public class JSON {
	@SuppressWarnings("unchecked")
	public static Object clone(Object in) throws JSONException {
		if(in instanceof JSONObject) {
			JSONObject out=new JSONObject();
			Iterator ks=((JSONObject)in).keys();
			while(ks.hasNext()) {
				String k=(String)ks.next();
				out.put(k,clone(((JSONObject)in).get(k)));
			}
			return out;
		} else if(in instanceof JSONArray) {
			JSONArray out=new JSONArray();
			int len=((JSONArray)in).length();
			for(int i=0;i<len;i++)
				out.put(clone(((JSONArray)in).get(i)));
			return out;
		} else {
			return in;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,String> stringObjectToMap(JSONObject in) throws JSONException {
		Map<String,String> out=new HashMap<String,String>();
		Iterator ks=in.keys();
		while(ks.hasNext()) {
			String k=(String)ks.next();
			out.put(k,in.getString(k));
		}
		return out;
	}
}
