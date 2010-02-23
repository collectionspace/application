package org.collectionspace.chain.csp.config.impl.main;

import java.util.Map;

import org.collectionspace.chain.csp.config.Section;
import org.collectionspace.chain.csp.config.SectionGenerator;

public class DefaultStep implements SectionGenerator {

	DefaultStep() {}
	
	public void step(Section milestone, Map<String, String> data) {
		for(Map.Entry<String,String> e : data.entrySet()) {
			milestone.addValue(e.getKey(),e.getValue());
		}
	}
}
