package org.collectionspace.chain.util.json;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONUtils {
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
	public static boolean checkJSONEquiv(Object a,Object b) throws JSONException {
		if(a==null) {
			if(b!=null)
				return false;
			return true;
		}
		if((a instanceof Number) || (a instanceof Boolean) || (a instanceof String)) {
			if(!a.equals(b))
				return false;
			return true;
		}
		if(a instanceof JSONArray) {
			if(!(b instanceof JSONArray))
				return false;
			if(((JSONArray)a).length()!=((JSONArray)b).length())
				return false;
			for(int i=0;i<((JSONArray) a).length();i++) {
				if(!checkJSONEquiv(((JSONArray) a).get(i),((JSONArray) b).get(i)))
					return false;
			}
			return true;
		}
		if(!(a instanceof JSONObject) || !(b instanceof JSONObject))
			return false;
		if(!checkKeys((JSONObject)a,(JSONObject)b))
			return false;
		if(((JSONObject)a).length()!=((JSONObject)b).length())
			return false;
		Iterator t=((JSONObject)a).keys();
		while(t.hasNext()) {
			String key=(String)t.next();
			if(!((JSONObject)b).has(key)) {
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
			out.put(key,value);
		}
		return out;
	}
	
	public static boolean checkJSONEquivOrEmptyStringKey(JSONObject a,JSONObject b) throws JSONException {
		return checkJSONEquiv(stripEmptyStringKey(a),stripEmptyStringKey(b));
	}
}
