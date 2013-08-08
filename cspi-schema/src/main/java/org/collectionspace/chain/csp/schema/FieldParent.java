/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

public interface FieldParent {
	public Record getRecord();
	public boolean isExpander();
	public String enumBlankValue();
	public String getID();
	public FieldParent getParent();
	public boolean isTrueRepeatField(); // Sometimes fields are of type "Repeat" only so they can be search with multiple values
}
