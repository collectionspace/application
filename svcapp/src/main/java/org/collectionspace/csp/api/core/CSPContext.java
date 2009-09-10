package org.collectionspace.csp.api.core;

import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.persistence.Storage;

public interface CSPContext {
	public void addConfigConsumer(ConfigConsumer cfg);
	public void addStorageType(String name,Storage store);
}
