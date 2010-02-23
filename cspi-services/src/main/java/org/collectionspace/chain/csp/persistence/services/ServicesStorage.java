/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;
import org.json.JSONObject;

/** The direct implementation of storage; only an instance of SplittingStorage which at the moment only splits
 * into ServicesCollectionObjectStorage.
 * 
 */
public class ServicesStorage extends SplittingStorage implements Storage {
	private CSPRequestCache cache;
	private ContextualisedStorage storage;
	
	public ServicesStorage(ContextualisedStorage storage,CSPRequestCache cache) {
		this.cache=cache;
		this.storage=storage;
	}
	
	public String getName() { return "persistence.services"; }

	public String autocreateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.autocreateJSON(cache, filePath, jsonObject);
	}

	public void createJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.createJSON(cache, filePath, jsonObject);
	}

	public void deleteJSON(String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.deleteJSON(cache,filePath);
	}

	public String[] getPaths(String rootPath,JSONObject restrictions) 
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.getPaths(cache,rootPath,restrictions);
	}

	public JSONObject retrieveJSON(String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return storage.retrieveJSON(cache,filePath);
	}

	public void updateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		storage.updateJSON(cache, filePath, jsonObject);
	}
}
