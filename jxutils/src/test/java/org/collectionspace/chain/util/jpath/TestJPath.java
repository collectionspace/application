package org.collectionspace.chain.util.jpath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJPath {

	private static final Logger log=LoggerFactory.getLogger(TestJPath.class);
	
	private void goodPath(String p) throws InvalidJPathException {
		JPathPath.compile(p);
	}

	private void badPath(String p) {
		try {
			JPathPath.compile(p);
			assertTrue(false);
		} catch (InvalidJPathException e) {
			log.info(e.getMessage());
		}
	}
	
	@Test public void basicJPathTest() throws InvalidJPathException {
		badPath("");
		badPath("hi");
		goodPath(".hi");
		goodPath(".hi[1]");
		goodPath(".hi[1].foo[4][2].bar");
		goodPath(".hi[1].foo[\"baz\"][2].bar");		
		goodPath(".hi[1].foo[\"baz\"][2].b\\u1aA4ar[\"\\r\\n\\f\\t\\\\\\/\\\"\\b\"]");
		badPath("..");
		badPath(".[1]");
		badPath("[]");
		badPath("[\"]");
		badPath("[");
		goodPath(".\\u1AaF");
		badPath(".\\u123G");
		badPath(".\\u123");
		badPath(".\\n");
		badPath("[\"\\w\"]");
		goodPath("[\"hi\"]");
		badPath(".1");
		badPath(".x;");
		badPath("[1");
	}

	private void compare(Object[] a,String path) throws InvalidJPathException {
		JPathPath p=JPathPath.compile(path);
		Object[] b=p.getPath().toArray(new Object[0]);
		assertEquals(a.length,b.length);
		for(int i=0;i<a.length;i++)
			assertEquals(a[i],b[i]);
	}
	
	@Test public void parseJPathTest() throws InvalidJPathException {
		compare(new Object[]{"hi"},".hi");
		compare(new Object[]{"hi",1},".hi[1]");
		compare(new Object[]{"hi",1,"foo",4,2,"bar"},".hi[1].foo[4][2].bar");
		compare(new Object[]{"hi",1,"foo","baz",2,"bar"},".hi[1].foo[\"baz\"][2].bar");		
		compare(new Object[]{"hi",1,"foo","baz",2,"b\u1aa4ar","\r\n\f\t\\/\"\b"},
							 ".hi[1].foo[\"baz\"][2].b\\u1aA4ar[\"\\r\\n\\f\\t\\\\\\/\\\"\\b\"]");
		compare(new Object[]{"\u1aaf"},".\\u1AaF");
		compare(new Object[]{"hi"},"[\"hi\"]");
	}
	
	@Test public void stepTest() throws JSONException, InvalidJPathException {
		JPathPath p=JPathPath.compile(".a[2]");
		JSONObject data=new JSONObject("{\"a\":[null,false,true]}");
		JSONArray d1=(JSONArray) p.step(data,"a");
		Boolean d2=(Boolean) p.step(d1,2);		
		assertTrue(d2);
		Boolean d3=(Boolean) p.step(d1,1);		
		assertFalse(d3);
		assertNull(p.step(d1,"x"));
		assertNull(p.step(data,1));
		assertNull(p.step(d2,"x"));
	}
	
	@Test public void getTest() throws JSONException, InvalidJPathException {
		JSONObject data=new JSONObject("{\"a\":[null,false,true,\"z\",[7,8],{\"x\":\"y\"}]}");		
		assertEquals("z",JPathPath.compile(".a[3]").getString(data));
		JSONArray a1=JPathPath.compile(".a[4]").getJSONArray(data);
		assertEquals(2,a1.length());
		JSONObject b1=JPathPath.compile(".a[5]").getJSONObject(data);
		assertEquals("y",b1.getString("x"));
		assertFalse(JPathPath.compile(".a[1]").getBoolean(data));
	}
}
