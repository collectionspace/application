package org.collectionspace.chain.storage.services;

import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.NotExistException;
import org.collectionspace.chain.storage.Storage;
import org.json.JSONObject;

// XXX we assume it's an object for now: we need an abstraction for other kinds of things, in due course.
public class ServicesCollectionObjectStorage implements Storage {
	private ServicesConnection conn;
	
	public ServicesCollectionObjectStorage(ServicesConnection conn) {
		this.conn=conn;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException {
		// TODO Auto-generated method stub
	}

	public String[] getPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	public String retrieveJSON(String filePath) throws NotExistException {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException {
		// TODO Auto-generated method stub
	}
}
