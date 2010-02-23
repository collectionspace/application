package org.collectionspace.chain.csp.nconfig.impl.main;

import java.util.Map;

import org.collectionspace.chain.csp.nconfig.Section;
import org.collectionspace.chain.csp.nconfig.SectionGenerator;

public class DefaultStep implements SectionGenerator {

	DefaultStep() {}
	
	public void step(Section milestone, Map<String, String> data) {
		for(Map.Entry<String,String> e : data.entrySet()) {
			milestone.addValue(e.getKey(),e.getValue());
		}
	}
}
