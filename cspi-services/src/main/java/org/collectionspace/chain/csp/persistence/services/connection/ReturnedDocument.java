/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class returns documents and statuses */
public class ReturnedDocument implements Returned {
	private static final Logger log = LoggerFactory.getLogger(ReturnedDocument.class);
	private int status;
	private Document doc;

	ReturnedDocument() {
	}

	public Document getDocument() {
		return doc;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void setResponse(HttpMethod method, int status) throws IOException, DocumentException {
		this.status = status;
		InputStream stream = method.getResponseBodyAsStream();
		SAXReader reader = new SAXReader();
		if (status >= 400) {
			log.debug("Got error : " + IOUtils.toString(stream));
		}
		// TODO errorhandling
		Document out = null;
		Header content_type = method.getResponseHeader("Content-Type");
		if (content_type != null && "application/xml".equals(content_type.getValue())) {
			if (log.isDebugEnabled()) {
				ByteArrayOutputStream dump = new ByteArrayOutputStream();
				// TODO CSPACE-2552 add ,"UTF-8" to reader.read()?
				out = reader.read(new TeeInputStream(stream, dump));
				log.debug(dump.toString("UTF-8"));
			} else {
				// TODO CSPACE-2552 add ,"UTF-8" to reader.read()?
				out = reader.read(stream);
			}
		}
		stream.close();
		doc = out;
	}
}
