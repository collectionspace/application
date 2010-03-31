package org.collectionspace.csp.api.core;

public interface CSPRequestCredentials {
	public void setCredential(String key,Object value);
	public Object getCredential(String key);
	public void removeCredential(String key);
}
