/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config;

/** method to just return the string provided (a fallback) */
public class DefaultConfigLoadMethod extends StringReadingConfigLoadMethod implements ConfigLoadMethod {
	@Override
	protected String string_get(String value) { return value; }
}
