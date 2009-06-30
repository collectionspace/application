package org.collectionspace.chain.storage.services;

import java.io.IOException;

import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.SplittingStorage;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.dom4j.DocumentException;
import org.json.JSONObject;

public class ServicesStorage extends SplittingStorage {

	public ServicesStorage(String base_url) throws InvalidJXJException, DocumentException, IOException {
		ServicesConnection conn=new ServicesConnection(base_url);
		addChild("collection-object",new ServicesCollectionObjectStorage(conn));
	}
}
