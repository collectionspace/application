/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

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
