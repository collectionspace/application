/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/** Utility class returns documents and statuses */
public class ReturnedMultipartDocument implements Returned {
	private int status;
	private Map<String,Document> docs=new HashMap<String,Document>();
	
	ReturnedMultipartDocument() {}

	public String[] listDocuments() { return docs.keySet().toArray(new String[0]); }
	public Document getDocument(String name) { return docs.get(name); }
	public int getStatus() { return status; }

	private void addDocument(String name,Document doc) { docs.put(name,doc); }	
	
	public void setResponse(HttpMethod method, int status) throws Exception {
		this.status=status;
		if(status<300) {
			InputStream stream=method.getResponseBodyAsStream();
			// XXX Could be a Stream if we'd written the data source
			DataSource ds=new UTF8StringDataSource(method.getResponseBodyAsString(),"multipart/mixed");
			MimeMultipart mmp=new MimeMultipart(ds);
			for(int i=0;i<mmp.getCount();i++) {
				BodyPart part=mmp.getBodyPart(i);
				String label=part.getHeader("label")[0];
				InputStream main=part.getInputStream();
				SAXReader reader=new SAXReader();
				// TODO errorhandling
				Document doc=null;
				String[] content_type=part.getHeader("Content-Type");
				if(content_type!=null && content_type.length>0 && "application/xml".equals(content_type[0])) {
					doc=reader.read(new TeeInputStream(main,System.err));
					System.err.println("RECEIVING "+label+" "+doc.asXML());
				}
				System.err.println("ok");
				addDocument(label,doc);
				main.close();
			}
			stream.close();
		}
	}
}
