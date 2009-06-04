package org.collectionspace.chain.storage.services;

import org.dom4j.Document;

public class ReturnedURL {
	private int status;
	private String url;
	
	ReturnedURL(int status,String url) {
		this.status=status;
		this.url=url;
	}
	
	public String getURL() { return url; }
	public int getStatus() { return status; }
}
