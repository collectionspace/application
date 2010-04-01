package org.collectionspace.csp.api.core;

public interface CSPRequestCache {
	public Object getCached(Class<?> klass,String[] name);
	public Object removeCached(Class<?> klass,String[] name);
	public void setCached(Class<?> klass,String[] name,Object value);
	public void reset();
}
