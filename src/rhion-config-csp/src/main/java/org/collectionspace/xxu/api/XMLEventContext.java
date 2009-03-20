package org.collectionspace.xxu.api;

public interface XMLEventContext {
	public String[] getStack();
	public void truncate(int num);
	public String[] getPreStack();
	public void restore(XMLEventContext in);
}
