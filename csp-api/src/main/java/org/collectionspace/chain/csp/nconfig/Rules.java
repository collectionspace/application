package org.collectionspace.chain.csp.nconfig;

public interface Rules {	
	public void addRule(String start,String[] path,String end,SectionGenerator step,Target target);
}
