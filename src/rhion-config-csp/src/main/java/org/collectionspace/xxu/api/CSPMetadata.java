package org.collectionspace.xxu.api;

public interface CSPMetadata {
	public String getIdentity();
	public int getMajorVersion();
	public int getMinorVersion();
	public CSPDependency[][] getDependencies();
	public String getHumanID();
	public String getHumanTitle();
	public String getHumanDescription();
	public String getCSPURL();
	public String getAuthor();
	public String getAuthorURL();
	public CSPProvider[] getProvider();
}
