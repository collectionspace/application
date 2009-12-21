package org.collectionspace.chain.csp.persistence.services;

import org.apache.commons.httpclient.HttpMethod;

public interface Returned {
	public void setResponse(HttpMethod method,int status) throws Exception;
}
