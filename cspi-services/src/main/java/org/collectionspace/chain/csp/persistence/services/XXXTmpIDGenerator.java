package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.util.Random;

import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;

public class XXXTmpIDGenerator implements Storage {
	private Random rnd=new Random();

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
		JSONObject out=new JSONObject();
		try {
			out.put("next",Integer.toString(rnd.nextInt()));
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSONException serialisng value",e);
		}
		return out;
	}
}
