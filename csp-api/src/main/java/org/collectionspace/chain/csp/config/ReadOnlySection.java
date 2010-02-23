package org.collectionspace.chain.csp.config;

public interface ReadOnlySection {
	public String getName();
	public Object getValue(String key);
	public ReadOnlySection getParent();
	public ReadOnlySection[] getChildren();
}
