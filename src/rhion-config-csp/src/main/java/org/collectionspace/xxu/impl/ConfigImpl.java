package org.collectionspace.xxu.impl;

import java.util.Map;
import java.util.HashMap;
import org.collectionspace.xxu.api.Config;
import org.collectionspace.xxu.api.CSPConfig;

public class ConfigImpl implements CSPConfig {	
	private Map<Object,CSPConfig> steps=new HashMap<Object,CSPConfig>();
	private Map<Object,Object> values=new HashMap<Object,Object>();	
	
	public void attach(Object in,CSPConfig value) { steps.put(in,value); }
	public void set(Object in,Object value) { values.put(in,value); }
	
	public Object getValue(Object[] in) {
		CSPConfig out=this;
		for(int i=0;i<in.length-1;i++)
			out=out.step(in[i]);
		return out.getValueStep(in[in.length-1]);
	}

	public Object getValueStep(Object in) { return values.get(in); }

	public CSPConfig step(Object in) { return steps.get(in); }

	public CSPConfig steps(Object[] in) {
		CSPConfig out=this;
		for(Object step : in)
			out=out.step(step);
		return out;
	}

	public void dump(StringBuffer out,String prefix) {
		for(Map.Entry<Object,Object> e : values.entrySet()) {
			out.append(prefix+"."+e.getKey()+"="+e.getValue()+" ");
		}
		for(Map.Entry<Object,CSPConfig> e : steps.entrySet()) {
			if(e.getValue() instanceof ConfigImpl)
				((ConfigImpl)e.getValue()).dump(out,prefix+"."+e.getKey());
			else
				out.append(prefix+"."+e.getKey()+"="+e.getValue());
		}
	}
	
	public String toString() { 
		StringBuffer out=new StringBuffer();
		out.append('{');
		dump(out,"");
		out.append('}');
		return out.toString();
	}
}
