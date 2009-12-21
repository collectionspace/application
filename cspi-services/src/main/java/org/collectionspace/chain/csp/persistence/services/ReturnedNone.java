package org.collectionspace.chain.csp.persistence.services;

import org.apache.commons.httpclient.HttpMethod;

public class ReturnedNone implements Returned {
	private int status;
	
	public void setResponse(HttpMethod method, int status) throws Exception {
		this.status=status;
	}
	
	public int getStatus() { return status; }
}
