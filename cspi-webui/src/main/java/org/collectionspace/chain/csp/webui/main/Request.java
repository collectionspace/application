package org.collectionspace.chain.csp.webui.main;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIRequest;

class Request {
	private CSPRequestCache cache;
	private Storage storage;
	private UIRequest uir;
	
	Request(CSPRequestCache cache,Storage storage,UIRequest ui) {
		this.cache=cache;
		this.storage=storage;
		this.uir=ui;
	}
	
	CSPRequestCache getCache() { return cache; }
	Storage getStorage() { return storage; }
	UIRequest getUIRequest() { return uir; }
}
