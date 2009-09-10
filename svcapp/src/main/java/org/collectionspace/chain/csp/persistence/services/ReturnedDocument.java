/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import org.dom4j.Document;

/** Utility class returns documents and statuses */
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
