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


public class ReturnUnknown  implements Returned {
	private static final Logger log=LoggerFactory.getLogger(ReturnedDocument.class);
	private int status;
	private Document doc;
	private byte[] bytebody;
	private String contentType;
	private String contentDisposition;
	
	ReturnUnknown() {}

	public Document getDocument() { return doc; }
	public  byte[] getBytes() { return bytebody; }
	public String getContentType() { return contentType; }
	public String getContentDisposition() { return contentDisposition; }
	@Override public int getStatus() { return status; }

	public void setResponse(HttpMethod method, int status) throws IOException, DocumentException {
		this.status=status;
		InputStream stream = method.getResponseBodyAsStream();
		if (status >= 400 || stream == null) {
			log.error("Error get content with HTTP Status code:" + Integer.toString(status) + "Got error: " + stream != null ? IOUtils.toString(stream) : "<emtpy content>");
		}
		
		// TODO errorhandling
		Header content_disp=method.getResponseHeader("Content-Disposition");
		contentDisposition = content_disp==null?"":content_disp.getValue();
		Header content_type=method.getResponseHeader("Content-Type");
		contentType = content_type.getValue();
		if (content_type != null) {
	        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	        IOUtils.copy(stream, byteOut);
	        new TeeInputStream(stream, byteOut); // REM - Is this needed?  Is there some necessary side-effect going on here by creating this TeeInputStream instance?
	        bytebody = byteOut.toByteArray();
		} else {
			log.error("Encountered a document with unknown content type.  Returning no content.");
		}
		
		stream.close();
	}
}
