package org.collectionspace.csp.api.config;

public interface ConfigNode {
	public ConfigNode getMainConfigNode(Object[] path);
	public Object getValue(Object[] path);
	public String dump();
}
