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
		generators.put("intake","INTAKE_NUMBER");
		generators.put("objects","ACCESSION_NUMBER");
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
