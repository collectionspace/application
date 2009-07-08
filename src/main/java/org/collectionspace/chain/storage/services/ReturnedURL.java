/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.storage.services;

/** Utility class returns URLs and statuses */
public class ReturnedURL {
	private int status;
	private String url;
	
	ReturnedURL(int status,String url) {
		this.status=status;
		this.url=url;
	}
	
	public String getURL() { return url; }
	public int getStatus() { return status; }
	
	public String getURLTail() {
		int last=url.lastIndexOf("/");
		if(last==-1)
			return url;
		return url.substring(last+1);
	}
}
