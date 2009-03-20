package org.collectionspace.xxu.js.impl.rhino;

public interface Sequence {
	public Object getThing();
	public int length();
	public Object getIndex(int i);
	public void setIndex(int idx,Object value);
	public boolean truncate(int idx);
	public String stringify();
}
