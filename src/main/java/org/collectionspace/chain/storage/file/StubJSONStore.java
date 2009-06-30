package org.collectionspace.chain.storage.file;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.storage.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;


public class StubJSONStore implements Storage {
	private String store_root;
	private static final Random rnd=new Random();
	
	/** Generate a file from a path.
	 * 
	 * @param path the path
	 * @return the file
	 */
	private File fileFromPath(String path) {
		path=path.replaceAll("[^A-Za-z0-9_,.-]","");
		return new File(store_root,path);
	}
	
	public String getStoreRoot() { return store_root; }
	
	/** Create stub store based on filesystem
	 * 
	 * @param store_root path to store
	 */
	public StubJSONStore(String store_root) {
		this.store_root=store_root;
	}
	
	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#retrieveJson(java.lang.String)
	 */
	public String retrieveJSON(String filePath) throws ExistException {
		File jsonFile = fileFromPath(filePath);
		if (!jsonFile.exists()) {
			throw new ExistException("No such file: " + filePath);
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
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException {
		set(filePath,jsonObject,true);
	}
	
	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException {
		set(filePath,jsonObject,false);
	}
		
	private synchronized void set(String filePath, JSONObject jsonObject,boolean create) throws ExistException {
		System.out.println("file path:" + filePath);
		File jsonFile = fileFromPath(filePath);
		if(create && jsonFile.exists())
			throw new ExistException("Cannot create: already exists");
		if(!create && !jsonFile.exists())
			throw new ExistException("Cannot update: does not exist");
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
	
	public String[] getPaths() {
		File dir=new File(store_root);
		if(!dir.isDirectory())
			return new String[]{};
		List<String> out=new ArrayList<String>();
		for(String f : dir.list()) {
			out.add(f);
		}
		return out.toArray(new String[0]);
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException {
		while(true) {
			int tail=rnd.nextInt(Integer.MAX_VALUE);
			String filename=filePath+"/"+tail;
			try {
				set(filename,jsonObject,true);
				return Integer.toString(tail);
			} catch(ExistException e) {
				// Try again
			}
		}
	}
}
