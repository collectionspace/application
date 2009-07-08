/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.storage;

import java.util.HashMap;
import java.util.Map;

/** StorageRegistry contains top level storage implementations, to be selected among by configuration.
 * 
 */
public class StorageRegistry {
	private Map<String,Storage> storages=new HashMap<String,Storage>();
	
	public void addStorage(String key,Storage value) { storages.put(key,value); }
	public Storage getStorage(String key) { return storages.get(key); }
}
