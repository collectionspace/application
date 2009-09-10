package org.collectionspace.chain.controller;

import org.collectionspace.chain.uispec.SchemaStore;
import org.collectionspace.csp.api.persistence.Storage;

/* Ideally wouldn't exist */

public class ControllerGlobal {
	private Storage store=null;
	private SchemaStore schema=null;

	public ControllerGlobal(Storage store,SchemaStore schema) {
		this.store=store;
		this.schema=schema;
	}
	
	public Storage getStore() { return store; }
	public SchemaStore getSchema() { return schema; }
}
