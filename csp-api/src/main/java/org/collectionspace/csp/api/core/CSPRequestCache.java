/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.core;

public interface CSPRequestCache {
	public Object getCached(Class<?> klass,String[] name);
	public Object removeCached(Class<?> klass,String[] name);
	public void setCached(Class<?> klass,String[] name,Object value);
	public void reset();
}
