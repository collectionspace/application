package org.collectionspace.chain.jsonstore;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

/* We should probably store JSON as JSON. We'll still parse and serialise it, though.
 *  Sorry, that was my mistake, Avi, :(. -- dan
 */

public class StubJSONStore implements JSONStore {		
	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#retrieveJson(java.lang.String)
	 */
	public String retrieveJson(String filePath) throws JSONNotFoundException {
		File jsonFile = new File(filePath);
		if (!jsonFile.exists())
		{
			throw new JSONNotFoundException("No such file: " + filePath);
		}

		try {
			FileReader r=new FileReader(jsonFile);
			String data=IOUtils.toString(r);
			r.close();
			JSONObject jsonObject = new JSONObject(data);
			return jsonObject.toString();
		}
		catch (IOException ioe)
		{
			return "";
		}
		catch (JSONException je)
		{
			return "";			
		}
	}

	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#storeJson(java.lang.String, org.json.JSONObject)
	 */
	public void storeJson(String filePath, JSONObject jsonObject)
	{
		System.out.println("file path:" + filePath);
		File jsonFile = new File(filePath);

		System.out.println("storing json");
		try
		{
			FileWriter w=new FileWriter(jsonFile);
			IOUtils.write(jsonObject.toString(),w);
			w.close();
		}
		catch (IOException ioe)
		{
			return;
		}
	}
}
