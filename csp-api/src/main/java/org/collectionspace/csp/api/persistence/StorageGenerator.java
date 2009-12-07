package org.collectionspace.csp.api.persistence;

import org.collectionspace.csp.api.core.CSPRequestCache;

public interface StorageGenerator {
	public Storage getStorage(CSPRequestCache cache);
}
