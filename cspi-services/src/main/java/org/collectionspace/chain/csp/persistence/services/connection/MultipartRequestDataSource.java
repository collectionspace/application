package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.dom4j.Document;

public class MultipartRequestDataSource implements RequestDataSource {
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
				System.err.println(new String(indata.toByteArray()));
				data=new ByteArrayInputStream(indata.toByteArray());
				ctype=body_mime.getContentType();
			}
		} catch(Exception e) {
			throw new ConnectionException("Could not connect",e);
		}			

	}
	
	public String getMIMEType() { return ctype; }
	public InputStream getStream() { return data; }
}
