package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.InputStream;

import org.dom4j.Document;

public class DocumentUnknownRequestDataSource implements RequestDataSource {
	private InputStream data;
	
	public DocumentUnknownRequestDataSource(Document body) throws ConnectionException {
		data=ConnectionUtils.documentToStream(body);
	}
	
	public String getMIMEType() { return "image/jpeg"; }
	public InputStream getStream() { return data; }
}
