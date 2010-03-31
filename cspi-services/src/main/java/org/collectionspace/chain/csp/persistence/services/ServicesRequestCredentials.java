package org.collectionspace.chain.csp.persistence.services;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.csp.api.core.CSPRequestCredentials;

public class ServicesRequestCredentials implements CSPRequestCredentials {
	private Map<String,Object> data=new HashMap<String,Object>();
	
	public Object getCredential(String key) { return data.get(key); }
	public void removeCredential(String key) { data.remove(key); }
	public void setCredential(String key, Object value) { data.put(key,value); }
}
