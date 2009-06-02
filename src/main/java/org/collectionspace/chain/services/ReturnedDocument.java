package org.collectionspace.chain.services;

import org.dom4j.Document;

public class ReturnedDocument {
	private int status;
	private Document doc;
	
	ReturnedDocument(int status,Document doc) {
		this.status=status;
		this.doc=doc;
	}
	
	public Document getDocument() { return doc; }
	public int getStatus() { return status; }
}
