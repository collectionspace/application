package org.collectionspace.toronto1.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class Store {
	private File base;
	
	public Store() {
		File home=new File(System.getProperty("user.home"));
		base=new File(home,"toronto1");
		if(!base.exists())
			base.mkdir();
	}
	
	public void createEntry(String id,JSONObject data) throws IOException {
		FileOutputStream file=new FileOutputStream(new File(base,id));
		IOUtils.write(data.toString(),file);
		file.close();
	}
	
	public JSONObject getEntry(String id) throws IOException, JSONException {
		FileInputStream file=new FileInputStream(new File(base,id));
		String data=IOUtils.toString(file);
		file.close();
		return new JSONObject(data);
	}
	
	public String[] getAll() {
		return base.list();
	}
	
	// XXX support multiple
	private boolean isGoodMatch(Map<String,String> conditions,JSONObject data) {
		for(Map.Entry<String,String> e : conditions.entrySet()) {
			String v=data.optString(e.getKey());
			if(v==null)
				return false;
			if(!v.equals(e.getValue()))
				return false;
		}
		return true;
	}
	
	// XXX make efficient
	public String[] search(Map<String,String> conditions) throws IOException, JSONException { // XXX make more flexible
		List<String> out=new ArrayList<String>();
		for(String id : getAll()) {
			JSONObject data=getEntry(id);
			if(isGoodMatch(conditions,data))
				out.add(id);
		}
		return out.toArray(new String[0]);
	}
}
