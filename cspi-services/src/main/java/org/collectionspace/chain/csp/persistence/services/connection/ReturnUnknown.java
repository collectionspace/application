package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReturnUnknown implements Returned {
	private static final Logger log=LoggerFactory.getLogger(ReturnedDocument.class);
	private int status;
	private Document doc;
	private InputStream stream;
	private byte[] byteArray = null;
	private String contentType;
	private String contentDisposition;
	private HttpMethod parentMethod = null;
	
	ReturnUnknown() {}

	public Document getDocument() { return doc; }
	public String getContentType() { return contentType; }
	public String getContentDisposition() { return contentDisposition; }
	@Override public int getStatus() { return status; }

	@Override
	public boolean setResponse(HttpMethod method, int status) throws IOException, DocumentException {
		boolean result = false; // it's NOT ok to release the parent connection since we don't consume the entire response stream here
		
		this.parentMethod = method;  // save a reference to the parent connection so we can release it later
		this.status = status;
		this.stream = method.getResponseBodyAsStream();
		if (status >= 400 || stream == null) {
			result = true;  // it's ok to release the parent connection since we have a bad response
			log.error("Error get content with HTTP Status code:" + Integer.toString(status) + "Got error: " + stream != null ? IOUtils.toString(stream) : "<emtpy content>");
		}
		
		// TODO errorhandling
		Header content_disp = method.getResponseHeader("Content-Disposition");
		contentDisposition = content_disp == null ? "" : content_disp.getValue();
		Header content_type=method.getResponseHeader("Content-Type");
		contentType = content_type.getValue();
		
		return result;
	}
	
	public InputStream getBodyAsStream() {
		return stream; 
	}
	
	/*
	 * Uses the response stream to create a byte array stream then closes the response stream and the parent connection.  Subsequent calls
	 * simply return a byte array from the byte array stream.
	 */
	public byte[] getBytes() {		
		if (byteArray == null) {
			try {
				byteArray = IOUtils.toByteArray(stream);
				stream = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			parentMethod.releaseConnection();  // now that we've processed the response stream
		}
		
		return byteArray;
	}
}
