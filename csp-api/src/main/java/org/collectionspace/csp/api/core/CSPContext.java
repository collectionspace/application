package org.collectionspace.csp.api.core;

import org.collectionspace.chain.csp.nconfig.NConfigRoot;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.api.ui.UI;

public interface CSPContext {
	// Config
	public void addConfigRules(NConfigurable cfg);
	public void setNConfigRoot(NConfigRoot cfg);
	public NConfigRoot getNConfigRoot();
	
	// Storage
	public void addStorageType(String name,StorageGenerator store);
	
	// UI
	public void addUI(String name,UI ui);
	
	/* Only after configuration */
	
	// Storage
	public StorageGenerator getStorage(String name);
	
	// UI
	public UI getUI(String name);
}
