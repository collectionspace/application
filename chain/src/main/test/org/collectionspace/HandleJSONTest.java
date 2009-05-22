package org.collectionspace;

import static org.junit.Assert.*;

import java.io.File;

import org.collectionspace.chain.jsonstore.ExistException;
import org.collectionspace.chain.jsonstore.JSONNotFoundException;
import org.collectionspace.chain.jsonstore.JSONStore;
import org.collectionspace.chain.jsonstore.StubJSONStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class HandleJSONTest {
	
	private final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
	                        "\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
	                        "{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
	                        "\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
	                        "\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
	                        "\"param\":\"type\"}]}";
	
	private JSONStore store;
	
	@Before public void setup() {
		File tmp=new File(System.getProperty("java.io.tmpdir"));
		File dir=new File(tmp,"ju-cspace");
		if(!dir.exists())
			dir.mkdir();
		// XXX do it properly when we have delete
		for(File f : dir.listFiles()) {
			f.delete();
		}
		store=new StubJSONStore(dir.toString());
	}

	
	@Test public void writeJSONToFile() throws JSONException, ExistException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/objects/json1.test", jsonObject);
	}
	
	@Test public void readJSONFromFile() throws JSONNotFoundException, JSONException, ExistException {
		JSONObject jsonObject = new JSONObject(testStr);
		store.createJSON("/objects/json1.test", jsonObject);
		String result = store.retrieveJson("/objects/json1.test");
		JSONObject resultObj = new JSONObject(result);
		JSONObject testObj = new JSONObject(testStr);
		assertTrue(resultObj.toString().equals(testObj.toString()));
	}

	@Test public void testJSONNotExist() throws JSONException {
		try
		{
			String result = store.retrieveJson("nonesuch.json");
			new JSONObject(result);
			assertTrue(false);
		}
		catch (JSONNotFoundException onfe) {}
	}		
}
