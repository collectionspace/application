/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;

public class Request {
	private CSPRequestCache cache;
	private StorageGenerator storage_generator;
	private Storage storage=null;
	private UIRequest uir;
	
	Request(StorageGenerator storage_generator,CSPRequestCache cache,UIRequest ui) {
		this.cache=cache;
		this.storage_generator=storage_generator;
		this.uir=ui;
	}
	
	private CSPRequestCredentials generateCredentials(UISession session) {
		CSPRequestCredentials creds=storage_generator.createCredentials(); // XXX
		if(session!=null && creds!=null) {
			String userId = (String) session.getValue(UISession.USERID);
			if (userId == null || userId.isEmpty() || userId.trim().isEmpty()) {
				userId = "<No User ID specified.>";
			}
			creds.setCredential(StorageGenerator.CRED_USERID, userId);
			
			String password = (String) session.getValue(UISession.PASSWORD);
			if (password == null || password.isEmpty() || password.trim().isEmpty()) {
				password = "<No password specified.>";
			}
			creds.setCredential(StorageGenerator.CRED_PASSWORD, password);
		}
		return creds;
	}
	
	public CSPRequestCache getCache() { return cache; }
	public synchronized Storage getStorage() throws UIException { 
		if(storage==null) {
			CSPRequestCredentials creds=generateCredentials(uir.getSession());
			storage=storage_generator.getStorage(creds,cache);
		}
		return storage;
	}
	public UIRequest getUIRequest() { return uir; }
	
	public void reset() {
		storage=null;
		cache.reset();
	}
}
