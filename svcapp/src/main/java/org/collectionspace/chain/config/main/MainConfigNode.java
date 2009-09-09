package org.collectionspace.chain.config.main;

public interface MainConfigNode {
	public MainConfigNode getMainConfigNode(Object[] path);
	public Object getValue(Object[] path);
	public String dump();
}
