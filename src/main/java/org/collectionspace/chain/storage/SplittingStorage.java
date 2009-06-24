package org.collectionspace.chain.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

// XXX test me
public class SplittingStorage implements Storage {
	private Map<String,Storage> children=new HashMap<String,Storage>();
	
	public void addChild(String prefix,Storage store) {
		children.put(prefix,store);
	}
	
	private Storage get(String path) throws ExistException {
		Storage out=children.get(path);
		if(out==null)
			throw new ExistException("No child storage bound to "+path);
		return out;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException {
		String parts[]=filePath.split("/",2);
		get(parts[0]).createJSON(parts[1],jsonObject);
	}

	public String[] getPaths() {
		List<String> out=new ArrayList<String>();
		for(Map.Entry<String,Storage> e : children.entrySet()) {
			for(String s : e.getValue().getPaths()) {
				out.add(e.getKey()+"/"+s);
			}
		}
		return out.toArray(new String[0]);
	}

	public String retrieveJSON(String filePath) throws ExistException {
		String parts[]=filePath.split("/",2);
		return get(parts[0]).retrieveJSON(parts[1]);
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException {
		String parts[]=filePath.split("/",2);
		get(parts[0]).updateJSON(parts[1],jsonObject);
	}
}
