package org.collectionspace.chain.csp.config;

public interface Rules {	
	public void addRule(String start,String[] path,String end,SectionGenerator step,Target target);
}
