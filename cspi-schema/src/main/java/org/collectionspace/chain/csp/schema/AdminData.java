/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

/**
 * all admin specific data from the cspace-config.xml file that is parsed when the server starts up
 * 
 * Everything the App layer needs to complete it's admin functions
 * 
 * @author caret
 *
 */
public class AdminData {
	String username,password;

	public AdminData(Spec spec, ReadOnlySection section) {
		username=(String)section.getValue("/username");
		password=(String)section.getValue("/password");
	}
	
/* XXX hopefully the service layer will change so we don't have to have login details hardcoded */
	public String getAuthUser() { return username; }
	public String getAuthPass() { return password; }
	

	public AdminData getAdminData() { return this; }
}
