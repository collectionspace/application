package org.collectionspace.csp.api.persistence;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;

public interface StorageGenerator {
	public static final String CRED_USERID="userid";
	public static final String CRED_PASSWORD="password";
	
	public Storage getStorage(CSPRequestCredentials credentials,CSPRequestCache cache);
	public CSPRequestCredentials createCredentials();
}
