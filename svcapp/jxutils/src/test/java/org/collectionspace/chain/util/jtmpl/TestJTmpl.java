package org.collectionspace.chain.util.jtmpl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TestJTmpl {
	private JSONObject getJSON(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		assertNotNull(stream);
		String data=IOUtils.toString(stream);
		stream.close();		
		return new JSONObject(data);
	}
	
	@Test public void testJTmplBasic() throws Exception {
		JSONObject data=getJSON("tmpl2.json");
		JTmplTmpl tmpl=JTmplTmpl.compile(getJSON("tmpl1.json"));
		JTmplDocument doc=tmpl.makeDocument();
		doc.set("b","BBB");
		doc.set("f",data.getJSONArray("a"));
		doc.set("g",data.getJSONObject("b"));
		doc.set("d","d");
		// {"a":{"d":"d","c":"\u0000","b":"BBB","e":[["1","2","3"],{"yyy":"zzz"},"h"]}}
		JSONObject obj1=doc.getJSON();
		assertNotNull(obj1);
		JSONObject obj2=obj1.getJSONObject("a");
		assertNotNull(obj2);
		assertEquals("d",obj2.getString("d"));
		assertEquals("\0",obj2.getString("c"));
		assertEquals("BBB",obj2.getString("b"));		
		JSONArray arr1=obj2.getJSONArray("e");
		assertNotNull(arr1);
		JSONArray arr2=arr1.getJSONArray(0);
		assertNotNull(arr2);
		assertEquals("1",arr2.get(0));
		assertEquals("2",arr2.get(1));
		assertEquals("3",arr2.get(2));
		JSONObject obj3=arr1.getJSONObject(1);
		assertNotNull(obj3);
		assertEquals("zzz",obj3.getString("yyy"));
		assertEquals("h",arr1.get(2));
		
		System.err.println(doc.getJSON().toString());
	}
}
