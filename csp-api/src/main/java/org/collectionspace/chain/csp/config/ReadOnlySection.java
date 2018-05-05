/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config;

public interface ReadOnlySection {
	public String getName();
	public Object getValue(String key);
	public Object getRawValue(String key);
	public ReadOnlySection getParent();
	public ReadOnlySection[] getChildren();
}
