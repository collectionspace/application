package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ArraySequence implements Sequence {
	private Object array;
	
	ArraySequence(Object in) { array=in; }
	
	public Object getThing() { return array; }
	public int length() { return Array.getLength(array); }
	public Object getIndex(int i) { return Array.get(array,i); }
	public void setIndex(int idx,Object value) { Array.set(array,idx,value); }
	public boolean truncate(int idx) { return false; }
	public String stringify() { return array.toString(); }
}
