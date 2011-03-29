/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.core;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.csp.api.core.CSPRequestCache;

public class RequestCache implements CSPRequestCache {
	private Map<String,Object> cache=new HashMap<String,Object>();

	private void lenStr(StringBuffer in,String add) {
		in.append(add.length());
		in.append(';');
		in.append(add);
	}
	
	private String generateKey(Class<?> klass, String[] name) {
		StringBuffer out=new StringBuffer();
		lenStr(out,klass.getName());
		for(String str : name)
			lenStr(out,str);
		return out.toString();
	}
	
	public Object getCached(Class<?> klass, String[] name) {
		return cache.get(generateKey(klass,name));
	}

	public Object removeCached(Class<?> klass, String[] name) {
		return cache.remove(generateKey(klass,name));
	}

	public void setCached(Class<?> klass, String[] name, Object value) {
		cache.put(generateKey(klass,name),value);
	}

	public void reset() {
		cache.clear();
	}
}
