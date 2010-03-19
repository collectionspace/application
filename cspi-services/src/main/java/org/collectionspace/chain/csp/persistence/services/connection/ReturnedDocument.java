/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class returns documents and statuses */
public class ReturnedDocument implements Returned {
	private static final Logger log=LoggerFactory.getLogger(ReturnedDocument.class);
	private int status;
	private Document doc;
	
	ReturnedDocument() {}
	
	public Document getDocument() { return doc; }
	public int getStatus() { return status; }

	public void setResponse(HttpMethod method, int status) throws IOException, DocumentException {
		this.status=status;
		log.info("response="+status);
		InputStream stream=method.getResponseBodyAsStream();
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		Document out=null;
		Header content_type=method.getResponseHeader("Content-Type");
		if(content_type!=null && "application/xml".equals(content_type.getValue())) {
			out=reader.read(new TeeInputStream(stream,System.err));
			log.info("RECEIVING "+out.asXML());
		}
		log.info("ok");
		stream.close();
		doc=out;
	}
}
