package org.collectionspace.csp.api.core;

import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.persistence.Storage;

public interface CSPContext {
	// Config
	public void addConfigConsumer(ConfigConsumer cfg);
	public void addConfigurable(Configurable cfg);

	// Storage
	public void addStorageType(String name,Storage store);
}
