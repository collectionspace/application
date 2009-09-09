package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.ConfigRoot;
import org.collectionspace.csp.api.config.ConfigListener;
import org.collectionspace.csp.api.config.Evaluator;

public class ConfigImpl extends ConfigNodeImpl implements ConfigRoot, ConfigListener {
	public void addConfig(Object[] path,Evaluator ev,boolean constant) {
		setValue(path,ev);
	}
}
