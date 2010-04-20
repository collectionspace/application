package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

/**
 * Holds all config details that are needed by the App layer to 
 * complete it's admin functions
 * @author csm22
 *
 */
public class AdminData {
	String username,password;

	public AdminData(Spec spec, ReadOnlySection section) {
		username=(String)section.getValue("/username");
		password=(String)section.getValue("/password");
	}
	

	public String getAuthUser() { return username; }
	public String getAuthPass() { return password; }
	

	public AdminData getAdminData() { return this; }
}
