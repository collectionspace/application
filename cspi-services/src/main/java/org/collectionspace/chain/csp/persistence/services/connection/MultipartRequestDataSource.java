/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipartRequestDataSource implements RequestDataSource {
	private static final Logger log = LoggerFactory
			.getLogger(MultipartRequestDataSource.class);
	private InputStream data;

	public MultipartRequestDataSource(Map<String, Document> body)
			throws ConnectionException {

		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("document");

		if (body != null) {
			for(String mapkey: body.keySet()){
				if(body.containsKey(mapkey) && body.get(mapkey)!=null){
					Element rooted = body.get(mapkey).getRootElement();
					root.add(rooted);
				}
			}
			data = ConnectionUtils.documentToStream(document);
		}

	}

	public String getMIMEType() {
		return "application/xml";
	}

	public InputStream getStream() {
		return data;
	}
}
