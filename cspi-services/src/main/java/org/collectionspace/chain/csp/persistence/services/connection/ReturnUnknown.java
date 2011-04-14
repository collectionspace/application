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
	private String contenttype;
	
	ReturnUnknown() {}

	public Document getDocument() { return doc; }
	public  byte[] getBytes() { return bytebody; }
	public String getContentType() { return contenttype; }
	public int getStatus() { return status; }

	public void setResponse(HttpMethod method, int status) throws IOException, DocumentException {
		this.status=status;
		InputStream stream=method.getResponseBodyAsStream();
		SAXReader reader=new SAXReader();
		if(status>=400) {
			log.error("Got error : "+IOUtils.toString(stream));
		}
		// TODO errorhandling
		Document out=null;
		Header content_type=method.getResponseHeader("Content-Type");
		contenttype = content_type.getValue();
		if(content_type!=null) {
			if(log.isDebugEnabled()) {
				ByteArrayOutputStream dump = new ByteArrayOutputStream();
				// TODO CSPACE-2552 add ,"UTF-8" to reader.read()?
				new TeeInputStream(stream,dump);
				log.debug(dump.toString());
			} else {

	            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	            IOUtils.copy(stream,byteOut);
	            new TeeInputStream(stream,byteOut);
	            bytebody = byteOut.toByteArray();
				
			//	out=reader.read(stream); 
			}
		}
		stream.close();
		//doc=out;
	}
}
