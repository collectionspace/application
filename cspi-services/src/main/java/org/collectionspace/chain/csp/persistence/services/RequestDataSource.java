package org.collectionspace.chain.csp.persistence.services;

import java.io.InputStream;

public interface RequestDataSource {
	public InputStream getStream();
	public String getMIMEType();
}
