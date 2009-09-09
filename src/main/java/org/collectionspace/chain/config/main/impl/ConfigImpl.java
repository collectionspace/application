package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.MainConfig;
import org.collectionspace.chain.config.main.csp.CSPConfigEvaluator;
import org.collectionspace.chain.config.main.csp.CSPRConfigResponse;

public class ConfigImpl extends ConfigNodeImpl implements MainConfig, CSPRConfigResponse {
	public void addConfig(Object[] path,CSPConfigEvaluator ev,boolean constant) {
		setValue(path,ev);
	}
}
