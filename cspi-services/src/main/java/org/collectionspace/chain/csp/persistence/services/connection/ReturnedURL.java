/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;

import org.collectionspace.csp.api.persistence.ExistException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class returns URLs and statuses */
public class ReturnedURL implements Returned {
	private static final Logger log=LoggerFactory.getLogger(ReturnedURL.class);
	private int status;
	private String url;
	
	ReturnedURL() {}
	
	public String getURL() { return url; }
	
	@Override public int getStatus() { return status; }
	
	public String getURLTail() {
		int last=url.lastIndexOf("/");
		if(last==-1)
			return url;
		return url.substring(last+1);
	}

	public void setResponse(HttpMethod method, int status) throws Exception {
		String possiblemessg = method.getResponseBodyAsString();
		Header location = method.getResponseHeader("Location");
		
		if (status == HttpStatus.SC_CONFLICT) {
			throw new ExistException("Record exists already. Can't create a duplicate: " + 
					possiblemessg != null ? possiblemessg : "Unknown reason.", status);
		}
		
		if (location == null) {
			if (possiblemessg != "") {
				throw new ConnectionException(possiblemessg, status, "");
			}
			throw new ConnectionException("Missing location header " + method.getResponseBodyAsString(), status, "");
		}
		url = location.getValue();
		this.status = status;
	}
	
	public void relativize(String base_url) {
		if(url.startsWith(base_url))
			url=url.substring(base_url.length());
	}
}
