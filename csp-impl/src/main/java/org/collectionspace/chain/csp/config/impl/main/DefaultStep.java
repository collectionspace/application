package org.collectionspace.chain.csp.config.impl.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.Section;
import org.collectionspace.chain.csp.config.SectionGenerator;

public class DefaultStep implements SectionGenerator {

	DefaultStep() {}
	
	@SuppressWarnings("unchecked")
	public void step(Section milestone, Map<String, String> data) {
		for(Map.Entry<String,String> e : data.entrySet()) {
			Object values=milestone.getValue(e.getKey());
			if(values==null) {
				milestone.addValue(e.getKey(),e.getValue());
			} else if(values instanceof String) {
				List<String> list=new ArrayList<String>();
				list.add((String)values);
				list.add(e.getValue());
				milestone.addValue(e.getKey(),list);
			} else if(values instanceof List) {
				((List<String>)values).add(e.getValue());
			}
		}
	}
}
