package org.collectionspace.chain.config.main;

public interface ConfigNode {
	public ConfigNode getMainConfigNode(Object[] path);
	public Object getValue(Object[] path);
	public String dump();
}
