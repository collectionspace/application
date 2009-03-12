package org.collectionspace.xxu.js.impl.rhino;

import java.util.List;

public class ListSequence implements Sequence {
	@SuppressWarnings("unchecked")
	private List list;
	
	@SuppressWarnings("unchecked")
	public ListSequence(List in) { list=in; }
	
	public Object getIndex(int i) { return list.get(i); }
	public Object getThing() { return list; }
	public int length() { return list.size(); }
	public void setIndex(int idx, Object value) { list.set(idx,value); }
	public String stringify() { return list.toString(); }
	public boolean truncate(int idx) { list.remove(idx); return true; }
}
