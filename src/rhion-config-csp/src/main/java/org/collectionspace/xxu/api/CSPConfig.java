package org.collectionspace.xxu.api;

public interface CSPConfig extends Config {
	public CSPConfig step(Object in);
	public CSPConfig steps(Object[] in);
	public void attach(Object in,CSPConfig value);
	public void set(Object in,Object value);
}
