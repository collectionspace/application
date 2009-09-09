package org.collectionspace.chain.config.main.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// In the end we will be more sophisticated about providing context here.
public class ConfigLoadingMessagesImpl implements ConfigLoadingMessages {
	private static final Logger log=LoggerFactory.getLogger(ConfigLoadingMessagesImpl.class);
	
	public void error(String message) { log.error(message); }
	public void warn(String message) { log.warn(message); }
}
