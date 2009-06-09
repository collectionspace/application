package org.collectionspace.chain.util;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONClone {
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
}
