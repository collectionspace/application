package org.collectionspace.chain.config.main.impl;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.csp.api.config.ConfigNode;
import org.collectionspace.csp.api.config.Evaluator;

public class ConfigNodeImpl implements ConfigNode {
	private Map<Object,ConfigNodeImpl> subnodes=new HashMap<Object,ConfigNodeImpl>();
	private Map<Object,Evaluator> values=new HashMap<Object,Evaluator>();
	private Evaluator default_value;
	
	void setDefaultValue(Evaluator in) { default_value=in; }
	void addValue(Object k,Evaluator v) { values.put(k,v); }
	void addConfigNode(Object k,ConfigNodeImpl v) { subnodes.put(k,v); }
	
	public ConfigNode getMainConfigNode(Object[] path) {
		return getMainConfigNode(path,0);
	}

	private ConfigNode getMainConfigNode(Object[] path,int start) {
		if(path.length<=start)
			return this;
		ConfigNodeImpl next=subnodes.get(path[start]);
		if(next==null)
			return null;
		return next.getMainConfigNode(path,start+1);
	}
	
	private Object getValue(Object[] path,int start) {
		if(path.length<=start)
			return default_value.getValue();
		if(path.length==start+1)
			return values.get(path[start]).getValue();
		ConfigNodeImpl next=subnodes.get(path[start]);
		if(next==null)
			return null;
		return next.getValue(path,start+1);
	}
	
	public Object getValue(Object[] path) {
		return getValue(path,0);
	}

	public void setValue(Object[] path,Evaluator value,int start) {
		if(path.length==0)
			return; // Cannot set value for root of config
		if(path.length==start+1)
			addValue(path[start],value);
		else {
			ConfigNodeImpl next=subnodes.get(path[start]);
			if(next==null) {
				next=new ConfigNodeImpl();
				subnodes.put(path[start],next);
			}
			next.setValue(path,value,start+1);
		}
	}
	
	public void setValue(Object[] path,Evaluator value) {
		setValue(path,value,0);
	}

	private void dump(StringBuffer out,String prefix) {
		for(Map.Entry<Object,Evaluator> e : values.entrySet()) {
			out.append(prefix+"/"+e.getKey()+" = "+e.getValue().getValue()+"\n");
		}
		for(Map.Entry<Object,ConfigNodeImpl> e : subnodes.entrySet()) {
			e.getValue().dump(out,prefix+"/"+e.getKey());
		}
	}
		
	public String dump() {
		StringBuffer out=new StringBuffer();
		dump(out,"");
		return out.toString();
	}
}
