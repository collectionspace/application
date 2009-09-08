package org.collectionspace.chain.config.main;

public interface MainConfigNode extends MainConfig {
	public MainConfigNode getMainConfigNode(Object[] path);
	public Object getValue(Object[] path);
}
