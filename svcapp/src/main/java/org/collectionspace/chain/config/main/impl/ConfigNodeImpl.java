package org.collectionspace.chain.config.main.impl;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.config.main.MainConfigNode;

public class ConfigNodeImpl implements MainConfigNode {
	private Map<Object,ConfigNodeImpl> subnodes=new HashMap<Object,ConfigNodeImpl>();
	private Map<Object,Object> values=new HashMap<Object,Object>();
	private Object default_value;
	
	void setDefaultValue(Object in) { default_value=in; }
	void addValue(Object k,Object v) { values.put(k,v); }
	void addConfigNode(Object k,ConfigNodeImpl v) { subnodes.put(k,v); }
	
	public MainConfigNode getMainConfigNode(Object[] path) {
		return getMainConfigNode(path,0);
	}

	private MainConfigNode getMainConfigNode(Object[] path,int start) {
		if(path.length<=start)
			return this;
		ConfigNodeImpl next=subnodes.get(path[start]);
		if(next==null)
			return null;
		return next.getMainConfigNode(path,start+1);
	}
	
	private Object getValue(Object[] path,int start) {
		if(path.length<=start)
			return default_value;
		if(path.length==start+1)
			return values.get(path[start]);
		ConfigNodeImpl next=subnodes.get(path[start]);
		if(next==null)
			return null;
		return next.getValue(path,start+1);
	}
	
	public Object getValue(Object[] path) {
		return getValue(path,0);
	}
}
