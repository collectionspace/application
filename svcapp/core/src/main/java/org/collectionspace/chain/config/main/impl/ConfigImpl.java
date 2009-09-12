package org.collectionspace.chain.config.main.impl;

import org.collectionspace.csp.api.config.ConfigListener;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Evaluator;

public class ConfigImpl extends ConfigNodeImpl implements ConfigRoot, ConfigListener {
	private static class StringEvaluator implements Evaluator {
		private String value;
		
		StringEvaluator(String in) { value=in; }
		public String getValue() { return value; }
	}
	
	public void addConfig(Object[] path,Evaluator ev,boolean constant) {
		setValue(path,ev);
	}

	public void addConfig(Object[] path,String value) {
		setValue(path,new StringEvaluator(value));
	}
}
