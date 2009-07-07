package org.collectionspace.chain.storage.services;

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
