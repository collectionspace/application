package org.collectionspace.chain.storage;

import java.util.HashMap;
import java.util.Map;

// XXX do something more flexible
public class StorageRegistry {
	private Map<String,Storage> storages=new HashMap<String,Storage>();
	
	public void addStorage(String key,Storage value) { storages.put(key,value); }
	public Storage getStorage(String key) { return storages.get(key); }
}
