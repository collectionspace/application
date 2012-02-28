/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.ui;

public interface UISession {
	public static final String USERID="session.cred.uid";
	public static final String PASSWORD="session.cred.password";
	public static final String TENANT = "session.cache.tenant";
	public static final String SEARCHTRAVERSER = "session.cache.searchtraverser";
	
	public Object getValue(String key);
	public void setValue(String key,Object value);
	public void deleteValue(String key);
	
}
