/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

/** Utility class returns URLs and statuses */
public class ReturnedURL implements Returned {
	private int status;
	private String url;
	
	ReturnedURL() {}
	
	public String getURL() { return url; }
	public int getStatus() { return status; }
	
	public String getURLTail() {
		int last=url.lastIndexOf("/");
		if(last==-1)
			return url;
		return url.substring(last+1);
	}

	public void setResponse(HttpMethod method, int status) throws Exception {
		System.err.println("response="+(method.getResponseBodyAsString()));
		Header location=method.getResponseHeader("Location");
		if(location==null)
			throw new ConnectionException("Missing location header");
		url=location.getValue();
		this.status=status;
	}
	
	public void relativize(String base_url) {
		if(url.startsWith(base_url))
			url=url.substring(base_url.length());
	}
}
