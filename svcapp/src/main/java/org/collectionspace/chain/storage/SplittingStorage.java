package org.collectionspace.chain.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

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
	
	private String[] split(String path,boolean missing_is_blank) throws ExistException {
		String[] out=path.split("/",2);
		if(out.length<2) {
			if(missing_is_blank)
				return new String[]{path,""};
			else
				throw new ExistException("Path is split point, not destination");
		}
		return out;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		if("".equals(parts[1])) { // autocreate?
			get(parts[0]).autocreateJSON("",jsonObject);
		}
		get(parts[0]).createJSON(parts[1],jsonObject);
	}

	public String[] getPaths() throws ExistException, UnimplementedException, UnderlyingStorageException {
		List<String> out=new ArrayList<String>();
		for(Map.Entry<String,Storage> e : children.entrySet()) {
			for(String s : e.getValue().getPaths()) {
				out.add(s);
			}
		}
		return out.toArray(new String[0]);
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		return get(parts[0]).retrieveJSON(parts[1]);
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).updateJSON(parts[1],jsonObject);
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,true);
		return get(parts[0]).autocreateJSON(parts[1],jsonObject);
	}

	public void deleteJSON(String filePath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		String parts[]=split(filePath,false);
		get(parts[0]).deleteJSON(parts[1]);		
	}
}
