package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringRequestDataSource implements RequestDataSource {
	private static final Logger log = LoggerFactory
			.getLogger(StringRequestDataSource.class);
	private InputStream data;
	private String ctype;

	public StringRequestDataSource(byte[] body, String type, String uploadName)
			throws ConnectionException {
		try {

			if (body != null) {
				MimeMultipart body_mime = new MimeMultipart();
				InternetHeaders headers = new InternetHeaders();
				headers.addHeader("Content-Type", "image/jpeg");
				headers.addHeader("Content-Disposition",
						" form-data; name=\"file\"; filename=\"" + uploadName
								+ "\"");
				BodyPart part = new MimeBodyPart(headers, body);
				body_mime.addBodyPart(part);
				ByteArrayOutputStream indata = new ByteArrayOutputStream();
				body_mime.writeTo(indata);
				log.debug("MultiPart request ");
				data = new ByteArrayInputStream(indata.toByteArray());
				ctype = body_mime.getContentType();
			}
		} catch (MessagingException e) {
			throw new ConnectionException("Could not connect"
					+ e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new ConnectionException("Could not connect"
					+ e.getLocalizedMessage(), e);
		}
	}

	public String getMIMEType() {
		// multipart/form-data
		// multipart/mixed
		String test = ctype;
		test = test.replace("multipart/mixed", "multipart/form-data");
		return test;
	}

	public InputStream getStream() {
		return data;
	}
}
