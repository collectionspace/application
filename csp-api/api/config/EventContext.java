package org.collectionspace.csp.api.config;

public interface EventContext {
	public String[] getStack();
	public String dumpStack();
	public String getPath();
	public EventContext getParentContext();
}
