package org.collectionspace.chain.csp.persistence.services;

import org.apache.commons.httpclient.HttpMethod;

public class ReturnedText implements Returned {
	private String text;
	
	public void setResponse(HttpMethod method, int status) throws Exception {
		text=method.getResponseBodyAsString();
	}

	public String getText() { return text; }
}
