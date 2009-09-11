package org.collectionspace.csp.api.config;

public interface ConfigListener {
	public void addConfig(Object[] path,Evaluator ev,boolean constant);
}
