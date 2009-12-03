package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;

public class ServicesIDGenerator implements Storage {
	private Random rnd=new Random();
	private ServicesConnection conn;
	
	private static final Map<String,String> generators=new HashMap<String,String>();
	
	static {
		generators.put("accession-activity","1a67470b-19b1-4ae3-88d4-2a0aa936270e");
		generators.put("objects","9dd92952-c384-44dc-a736-95e435c1759c"); // XXX temporary hack for venus
		generators.put("accession","9dd92952-c384-44dc-a736-95e435c1759c");
		generators.put("archive","70586d30-9dca-4a07-a3a2-1976fe898028");
		generators.put("evaluation","d2d80822-25c7-4c7c-a105-fc40cdb0c50f");
		generators.put("evaluations","d2d80822-25c7-4c7c-a105-fc40cdb0c50f");// XXX temporary hack for venus
		generators.put("intake","8088cfa5-c743-4824-bb4d-fb11b12847f7");
		generators.put("intake-object","a91db555-5c53-4996-9918-6712351397a0");
		generators.put("library","80fedaf6-1647-4f30-9f53-a75a3cac2ffd");
		generators.put("loans-in","ed87e7c6-0678-4f42-9d33-f671835586ef");
		generators.put("study","0518132e-dd8c-4773-8fa9-07c9af4444ee");
		generators.put("uuid","1fa40353-05b8-4ae6-82a6-44a18b4f3c12");
	}

	public ServicesIDGenerator(ServicesConnection conn) { this.conn=conn; }
	
	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void deleteJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public String[] getPaths(String rootPath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String val=conn.getTextDocument(RequestMethod.POST,"idgenerators/"+generators.get(filePath)+"/ids",null);
			JSONObject out=new JSONObject();
			out.put("next",val);
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSON exception",e);
		}
	}
}
