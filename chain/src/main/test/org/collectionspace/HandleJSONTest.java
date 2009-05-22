package org.collectionspace;

import org.collectionspace.chain.jsonstore.ExistException;
import org.collectionspace.chain.jsonstore.JSONNotFoundException;
import org.collectionspace.chain.jsonstore.JSONStore;
import org.collectionspace.chain.jsonstore.StubJSONStore;
import org.json.JSONException;
import org.json.JSONObject;

public class HandleJSONTest {
	
	private final static String testStr = "{\"items\":[{\"value\":\"This is an experimental widget being tested. It will not do what you expect.\"," +
	                        "\"title\":\"\",\"type\":\"caption\"},{\"title\":\"Your file\",\"type\":\"resource\",\"param\":\"file\"}," +
	                        "{\"title\":\"Author\",\"type\":\"text\",\"param\":\"author\"},{\"title\":\"Title\",\"type\":\"text\"," +
	                        "\"param\":\"title\"},{\"title\":\"Type\",\"type\":\"dropdown\",\"values\":[{\"value\":\"1\",\"text\":" +
	                        "\"thesis\"},{\"value\":\"2\",\"text\":\"paper\"},{\"value\":\"3\",\"text\":\"excel-controlled\"}]," +
	                        "\"param\":\"type\"}]}";
	
	
	public static void main(String[] args)
	{
		JSONStore store=new StubJSONStore("/");
		boolean success = true;
		
		// Test 1: write a json object to a file
		try
		{
			JSONObject jsonObject = new JSONObject(testStr);
			store.updateJSON("/schema/json1.test", jsonObject);
			System.out.println("test 1 succeeded.");
		}
		catch (JSONException je)
		{
			System.out.println("test 1 failed.");
			System.out.println("JSON Exception creating the JSON object from string: " + je);
			success = false;
		} catch (ExistException e) {
			System.out.println("test 1 failed.");
			System.out.println("ExistException: " + e);
			success = false;
		}
		
		// test 2: retrieve json object from file
		try
		{
			String result = store.retrieveJson("json1.test");
			JSONObject resultObj = new JSONObject(result);
			JSONObject testObj = new JSONObject(testStr);
			if (resultObj.toString().equals(testObj.toString()))
			{
				System.out.println("test 2 succeeded.");
			}
			else
			{
				System.out.println("test 2 failed.");
				success = false;
			}
		}
		catch (JSONException je)
		{
			System.out.println("JSON exception creating objects for comparison. " + je);
			System.out.println("test 2 failed.");
			success = false;
		}
		catch (Exception onfe)
		{
			System.out.println("Object not found. " + onfe);
			System.out.println("test 2 failed.");
			success = false;
		}

		// test 3: fail to retrieve non-existing json object
		try
		{
			String result = store.retrieveJson("nonesuch.json");
			new JSONObject(result);
			System.out.println("test 3 failed.");
			success = false;
		}
		catch (JSONNotFoundException onfe)
		{
			System.out.println("test 3 succeeded.");
		}
		catch (Exception e)
		{
			System.out.println("Unexpected exception: " + e);
			System.out.println("test 3 failed.");
			success = false;
		}

		System.out.print("Testing completed ");
		if (success)
		{
			System.out.println("successfully");
		}
		else
		{
			System.out.println("with some failures");
		}
		
	}
		
}
