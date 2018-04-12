/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config;

import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Configurable {
	public void configure(RuleSet rules) throws CSPDependencyException;
	public void config_finish() throws CSPDependencyException;
	public void complete_init(CSPManager cspManager, boolean forXsdGeneration) throws CSPDependencyException;
}
