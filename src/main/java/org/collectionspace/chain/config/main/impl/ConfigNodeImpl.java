package org.collectionspace.chain.config.main.impl;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.config.main.MainConfigNode;
import org.collectionspace.chain.config.main.csp.CSPConfigEvaluator;

public class ConfigNodeImpl implements MainConfigNode {
	private Map<Object,ConfigNodeImpl> subnodes=new HashMap<Object,ConfigNodeImpl>();
	private Map<Object,CSPConfigEvaluator> values=new HashMap<Object,CSPConfigEvaluator>();
	private CSPConfigEvaluator default_value;
	
	void setDefaultValue(CSPConfigEvaluator in) { default_value=in; }
	void addValue(Object k,CSPConfigEvaluator v) { values.put(k,v); }
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

	public void setValue(Object[] path,CSPConfigEvaluator value,int start) {
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
	
	public void setValue(Object[] path,CSPConfigEvaluator value) {
		setValue(path,value,0);
	}

	private void dump(StringBuffer out,String prefix) {
		for(Map.Entry<Object,CSPConfigEvaluator> e : values.entrySet()) {
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
