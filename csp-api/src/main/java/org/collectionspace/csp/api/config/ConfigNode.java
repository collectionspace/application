package org.collectionspace.csp.api.config;

public interface ConfigNode {
	public ConfigNode getMainConfigNode(Object[] path);
	public Object getValue(Object[] path);
	public boolean hasValue(Object[] path);
	public String getString(Object[] path) throws ConfigException;	
	public String dump();
}
