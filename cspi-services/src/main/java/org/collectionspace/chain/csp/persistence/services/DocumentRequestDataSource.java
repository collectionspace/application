package org.collectionspace.chain.csp.persistence.services;

import java.io.InputStream;

import org.dom4j.Document;



public class DocumentRequestDataSource implements RequestDataSource {
	private InputStream data;
	
	public DocumentRequestDataSource(Document body) throws ConnectionException {
		data=ConnectionUtils.documentToStream(body);
	}
	
	public String getMIMEType() { return "application/xml"; }
	public InputStream getStream() { return data; }
}
