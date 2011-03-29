/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.container.impl;

import org.collectionspace.csp.api.core.CSPDependencyException;

public interface Dependable {
	public void run() throws CSPDependencyException;
	public String getName();
}
