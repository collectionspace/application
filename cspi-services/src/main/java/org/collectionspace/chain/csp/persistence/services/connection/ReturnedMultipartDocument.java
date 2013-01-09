/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class returns documents and statuses */
public class ReturnedMultipartDocument implements Returned {
	private static final Logger log=LoggerFactory.getLogger(ReturnedMultipartDocument.class);
	private int status;
	private Map<String,Document> docs=new HashMap<String,Document>();
	
	ReturnedMultipartDocument() {}
        
        // Start of a range of HTTP status code values that signify error status
        final static int ERROR_STATUS_START_VALUE = 400;
        
        // Custom HTTP status code used by CollectionSpace's Services layer
        // to signify that a transaction failure has occurred
        public final static int TRANSACTION_FAILED_STATUS = 590;
        public final static String TRANSACTION_FAILED_MESSAGE =
            "A transaction failed, either because it timed out or due to some other cause. " +
            "Please contact your system administrator.";

	public String[] listDocuments() { return docs.keySet().toArray(new String[0]); }
	public Document getDocument(String name) { return docs.get(name); }
	private void setStatus(int status) { this.status = status; }
	public int getStatus() { return status; }
        public boolean isErrorStatus() {
           return ((getStatus() >= ERROR_STATUS_START_VALUE)? true : false);
        }
        public boolean isTransactionFailedStatus() {
           return ((getStatus() == TRANSACTION_FAILED_STATUS)? true : false);
        }

	private void addDocument(String name,Document doc) { docs.put(name,doc); }	
	
	
	public void setResponse(HttpMethod method, int status) throws Exception  {
		setStatus(status);
		InputStream stream=method.getResponseBodyAsStream();
		SAXReader reader=new SAXReader();
		if(isErrorStatus()) {
			log.info("Got error : "+IOUtils.toString(stream));
		}
		// TODO errorhandling
		Document doc=null;
		Header content_type=method.getResponseHeader("Content-Type");
		if(content_type!=null && "application/xml".equals(content_type.getValue())) {
			
			if(log.isDebugEnabled()) {
				ByteArrayOutputStream dump = new ByteArrayOutputStream();
				doc=reader.read(new TeeInputStream(stream,dump));
				log.debug(dump.toString("UTF-8"));
			} else {
				doc=reader.read(stream,"UTF-8"); 
			}
			//split up document
			Element root=doc.getRootElement();
			// iterate through child elements of root
	        for ( Iterator i = root.elementIterator(); i.hasNext(); ) {
	            Element element = (Element) i.next();
				addDocument(element.getName(),DocumentHelper.parseText( element.asXML() ));
	        }
		}
		stream.close();
	}
	
}
