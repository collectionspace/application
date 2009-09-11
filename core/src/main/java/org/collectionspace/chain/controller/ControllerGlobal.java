package org.collectionspace.chain.controller;

import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.impl.core.CSPManagerImpl;

/* Ideally wouldn't exist */

public class ControllerGlobal {
	private Storage store=null;
	private SchemaStore schema=null;
	private CSPManager csp_manager=new CSPManagerImpl();

	public ControllerGlobal(SchemaStore schema) {
		this.schema=schema;
	}
	
	public void setStore(Storage store) { this.store=store; }
	public Storage getStore() { return store; }
	public SchemaStore getSchema() { return schema; }
	
	public CSPManager getCSPManager() { return csp_manager; }
}
