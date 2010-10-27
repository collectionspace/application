/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;

public class Request {
	private CSPRequestCache cache;
	private Storage storage;
	private UIRequest uir;
	private WebUI wui;
	
	Request(WebUI wui,CSPRequestCache cache,Storage storage,UIRequest ui) {
		this.cache=cache;
		this.storage=storage;
		this.uir=ui;
		this.wui=wui;
	}
	
	public CSPRequestCache getCache() { return cache; }
	public Storage getStorage() throws UIException { 
		if(storage==null) {
			storage=wui.generateStorage(uir.getSession(),cache);
		}
		return storage;
	}
	public UIRequest getUIRequest() { return uir; }
	
	public void reset() {
		storage=null;
		cache.reset();
	}
}
