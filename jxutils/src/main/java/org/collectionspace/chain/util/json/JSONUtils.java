/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.json;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONUtils {
	final static String DISPLAYNAME_LIST_SEPARATOR = "__|__";
	final static String DISPLAYNAME_LIST_SEPARATOR_REGEX = "__\\|__";

	private static final Logger log=LoggerFactory.getLogger(JSONUtils.class);

	@SuppressWarnings("unchecked")
	private static Set<String> objectKeys(JSONObject a) {
		Set<String> out=new HashSet<String>();
		Iterator<String> t=(Iterator<String>)a.keys();
		while(t.hasNext())
			out.add(t.next());
		return out;
	}

	private static boolean printMissing(String prefix,Set<String> a,Set<String> b) {
		boolean good=true;
		Set<String> c=new HashSet<String>(a);
		c.removeAll(b);
		for(String s : c) {
			log.info(prefix +": "+s);
			good=false;
		}
		return good;
	}
	
	private static boolean checkKeys(JSONObject a,JSONObject b) throws JSONException {
		Set<String> ak=objectKeys(a);
		Set<String> bk=objectKeys(b);
		boolean good=printMissing("Missing from b",ak,bk);
		good = printMissing("Missing from a",bk,ak) && good;
		return good;
	}

	// XXX refactor
	@SuppressWarnings("unchecked")
	public static String checkKey(Object a, String test) throws JSONException {
		if(a==null) {
			return null;
		}
		if((a instanceof Number) || (a instanceof Boolean) || (a instanceof String)) {
			return null;
		}
		if(a instanceof JSONArray) {
			JSONArray bob = (JSONArray)a;
			for(int i=0;i<bob.length();i++) {
				return checkKey(bob.get(i), test);
			}
		}
		
		if(a instanceof JSONObject) {
			if(((JSONObject) a).has(test)){
				return ((JSONObject) a).getString(test);
			}

			Iterator t=((JSONObject)a).keys();
			while(t.hasNext()) {
				String key=(String)t.next();
				String temp = checkKey(((JSONObject)a).get(key),test);
				if (temp != null) {
					return temp;
				}
			}
		}
		
		return null;
	}
	// XXX refactor
	@SuppressWarnings("unchecked")
	public static boolean checkJSONEquiv(Object a,Object b) throws JSONException {
		if(a==null) {
			log.info("a is null");
			if(b!=null){
				log.info("b is not null");
				return false;
			}
			return true;
		}
		if((a instanceof Number) || (a instanceof Boolean) || (a instanceof String)) {
			if(!a.equals(b)){
				log.info("a != b");
				log.info(a.toString());
				log.info(b.toString());
				log.info("end");
				return false;
			}
			return true;
		}
		if(a instanceof JSONArray) {
			if(!(b instanceof JSONArray))
				return false;
			if(((JSONArray)a).length()!=((JSONArray)b).length()){
				log.info("array length diff a ="+ ((JSONArray)a).length()+": b= "+((JSONArray)b).length());
				log.info(((JSONArray)a).toString());
				return false;
			}
			
			for(int i=0;i<((JSONArray) a).length();i++) {
				if(!checkJSONEquiv(((JSONArray) a).get(i),((JSONArray) b).get(i))){
					log.info("array length diff 2" +((JSONArray) a).get(i));
					return false;
				}
			}
			return true;
		}
		
		if(!(a instanceof JSONObject) || !(b instanceof JSONObject)){
			log.info("One but not both are JSON objects - check for options");
			log.info(((JSONObject)a).toString());
			log.info(((JSONObject)b).toString());
			return false;
		}
		if(!checkKeys((JSONObject)a,(JSONObject)b))
			return false;
		if(((JSONObject)a).length()!=((JSONObject)b).length()) {
                        log.info("Lengths of two JSON objects don't match.");
			return false;
                }
		Iterator t=((JSONObject)a).keys();
		while(t.hasNext()) {
			String key=(String)t.next();
			if(!((JSONObject)b).has(key)) {
                                log.info("b missing key " + key);
				return false;
			}
			if(!checkJSONEquiv(((JSONObject)a).get(key),((JSONObject)b).get(key)))
				return false;
		}
		return true;
	}

	private static JSONObject stripEmptyStringKey(JSONObject in) throws JSONException {
		JSONObject out=new JSONObject();
		Iterator<?> t=in.keys();
		while(t.hasNext()) {
			String key=(String)t.next();
			Object value=in.get(key);
			if((value instanceof String) && "".equals(value))
				continue;
			if(value instanceof JSONObject) {	// recurse
				JSONObject out2=stripEmptyStringKey((JSONObject)value);
				out.put(key,out2);
				continue;
			}
			if(value instanceof JSONArray) {	// recurse
				JSONArray list = (JSONArray)value;
				JSONArray newlist = new JSONArray();
				for(int i=0;i<list.length();i++) {
					Object thing = list.get(i);
					if(thing instanceof JSONObject) {	// recurse
						JSONObject out3=stripEmptyStringKey((JSONObject)thing);
						newlist.put(out3);
					} else {
						newlist.put(thing);
					}
				}
				out.put(key,newlist);
				continue;
			}
			out.put(key,value);
		}
		return out;
	}
	
	public static boolean checkJSONEquivOrEmptyStringKey(JSONObject a,JSONObject b) throws JSONException {
		boolean result = checkJSONEquiv(stripEmptyStringKey(a),stripEmptyStringKey(b));
		
		if (log.isDebugEnabled() && result == false) {
			String staticJSON = a.toString();
			String generatedJSON = b.toString();
			log.debug("JSON objects did not compare successfully.");
			log.debug("staticJSON=" + staticJSON);
			log.debug("generatedJSON=" + generatedJSON);
		}
		
		return result;
	}
	
	// Hack to deal with holding arrays of Strings in the glean map which will later be JSON Array
	public static String appendWithArraySeparator(String existing, String toAppend) {
		return existing + DISPLAYNAME_LIST_SEPARATOR + toAppend; 
	}

	public static JSONArray createJSONArrayFromSeparatedString(String toSplit) {
		String[] values = toSplit.split(DISPLAYNAME_LIST_SEPARATOR_REGEX);
		JSONArray array = new JSONArray();
		for(String value:values) {
			array.put(value);
		}
		return array;
	}
}
