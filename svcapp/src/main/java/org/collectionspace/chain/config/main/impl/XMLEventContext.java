package org.collectionspace.chain.config.main.impl;

public interface XMLEventContext {
	public String[] getStack();
	public String dumpStack();
	public String getPath();
	public XMLEventContext getParentContext();
}
