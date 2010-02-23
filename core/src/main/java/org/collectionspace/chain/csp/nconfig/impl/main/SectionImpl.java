package org.collectionspace.chain.csp.nconfig.impl.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Section;
import org.collectionspace.chain.csp.nconfig.Target;

public class SectionImpl implements Section {
	private ReadOnlySection parent;
	private String name;
	private Map<String,Object> map=new HashMap<String,Object>();
	private List<SectionImpl> children=new ArrayList<SectionImpl>();
	private Target target;
	
	public SectionImpl(ReadOnlySection parent,String name,Target target) {
		this.parent=parent;
		this.name=name;
		this.target=target;
	}
	
	public ReadOnlySection getParent() { return parent; }
	public String getName() { return name; }
	public void addValue(String key,Object value) { map.put(key,value); }
	public Object getValue(String key) { return map.get(key); }
	void addChild(SectionImpl m) { children.add(m); }
	
	public void buildTargets(Object payload) {
		if(target!=null)
			payload=target.populate(payload,this);
		for(SectionImpl m : children)
			m.buildTargets(payload);
	}
	
	public void dump() {
		System.err.println("Dumping milestone type "+name);
		for(Map.Entry<String,Object> e : map.entrySet()) {
			System.err.println(" "+e.getKey()+"="+e.getValue());
		}
		for(SectionImpl m : children)
			m.dump();
	}

	public ReadOnlySection[] getChildren() {
		return children.toArray(new ReadOnlySection[0]);
	}
}
