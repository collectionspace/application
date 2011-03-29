/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.persistence;

import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;

public interface StorageGenerator {
	public static final String CRED_USERID="userid";
	public static final String CRED_PASSWORD="password";
	
	public Storage getStorage(CSPRequestCredentials credentials,CSPRequestCache cache);
	public CSPRequestCredentials createCredentials();
}
