package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIRequest;

public class Request {
	private CSPRequestCache cache;
	private Storage storage;
	private UIRequest uir;
	
	Request(CSPRequestCache cache,Storage storage,UIRequest ui) {
		this.cache=cache;
		this.storage=storage;
		this.uir=ui;
	}
	
	public CSPRequestCache getCache() { return cache; }
	public Storage getStorage() { return storage; }
	public UIRequest getUIRequest() { return uir; }
}
