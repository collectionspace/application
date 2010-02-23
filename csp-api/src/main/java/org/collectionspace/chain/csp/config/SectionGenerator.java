package org.collectionspace.chain.csp.config;

import java.util.Map;

public interface SectionGenerator {
	public void step(Section milestone,Map<String,String> data);
}
