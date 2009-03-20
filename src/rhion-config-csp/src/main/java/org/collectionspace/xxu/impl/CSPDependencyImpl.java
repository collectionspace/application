package org.collectionspace.xxu.impl;

import org.collectionspace.xxu.api.CSPDependency;

public class CSPDependencyImpl implements CSPDependency {
	private String id;
	private int major,minor;
	
	public CSPDependencyImpl(String id,int major,int minor) {
		this.id=id;
		this.major=major;
		this.minor=minor;
	}
	
	public String getIdentity() { return id; }
	public int getMajorVersion() { return major; }
	public int getMinimumMinorVersion() { return minor; }
}
