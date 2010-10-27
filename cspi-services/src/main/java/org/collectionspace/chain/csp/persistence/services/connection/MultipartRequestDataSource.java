/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipartRequestDataSource implements RequestDataSource {
	private static final Logger log=LoggerFactory.getLogger(MultipartRequestDataSource.class);
	private InputStream data;
	private String ctype;
	
	public MultipartRequestDataSource(Map<String,Document> body) throws ConnectionException {
		try {
			if(body!=null) {
				MimeMultipart body_mime=new MimeMultipart();
				for(Map.Entry<String,Document> e : body.entrySet()) {
					InternetHeaders headers=new InternetHeaders();
					headers.addHeader("label",e.getKey());
					headers.addHeader("Content-Type","application/xml");
					BodyPart part=new MimeBodyPart(headers,ConnectionUtils.documentToBytes(e.getValue()));
					body_mime.addBodyPart(part);
				}
				ByteArrayOutputStream indata=new ByteArrayOutputStream();
				body_mime.writeTo(indata);
				log.debug("MultiPart request ");
				data=new ByteArrayInputStream(indata.toByteArray());
				ctype=body_mime.getContentType();
			}
		} catch (MessagingException e) {
			throw new ConnectionException("Could not connect"+e.getLocalizedMessage(),e);
		} catch (IOException e) {
			throw new ConnectionException("Could not connect"+e.getLocalizedMessage(),e);
		}			

	}
	
	public String getMIMEType() { return ctype; }
	public InputStream getStream() { return data; }
}
