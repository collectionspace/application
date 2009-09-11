package org.collectionspace.chain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONTestUtil {
	// XXX refactor
	@SuppressWarnings("unchecked")
	public static void assertJSONEquiv(Object a,Object b) throws JSONException {
		if(a==null) {
			assertNull(b);
			return;
		}
		if((a instanceof Number) || (a instanceof Boolean) || (a instanceof String)) {
			assertEquals(a,b);
			return;
		}
		if(a instanceof JSONArray) {
			assertTrue(b instanceof JSONArray);
			assertEquals(((JSONArray) a).length(),((JSONArray)b).length());
			for(int i=0;i<((JSONArray) a).length();i++)
				assertJSONEquiv(((JSONArray) a).get(i),((JSONArray) b).get(i));
			return;
		}
		assertTrue(a instanceof JSONObject);
		assertTrue(b instanceof JSONObject);
		assertEquals(((JSONObject)a).length(),((JSONObject)b).length());
		Iterator t=((JSONObject)a).keys();
		while(t.hasNext()) {
			String key=(String)t.next();
			assertTrue(((JSONObject)b).has(key));
			assertJSONEquiv(((JSONObject)a).get(key),((JSONObject)b).get(key));
		}
	}
}
