/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// In the end we will be more sophisticated about providing context here.
public class ConfigLoadingMessagesImpl implements ConfigLoadingMessages {
	private static final Logger log=LoggerFactory.getLogger(ConfigLoadingMessagesImpl.class);
	
	public void error(String message) { log.error(message); }
	public void warn(String message) { log.warn(message); }
}
