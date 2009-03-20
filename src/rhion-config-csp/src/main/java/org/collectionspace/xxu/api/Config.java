package org.collectionspace.xxu.api;

public interface Config {
	public Config step(Object in);
	public Config steps(Object[] in);
	public Object getValueStep(Object in);
	public Object getValue(Object[] in);
}
