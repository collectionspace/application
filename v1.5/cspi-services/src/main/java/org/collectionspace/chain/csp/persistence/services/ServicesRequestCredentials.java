/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
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
