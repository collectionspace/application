/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;


import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;

/** The actual REST calls are handled by ServicesConnection, which uses utility types ReturnedDocument and ReturnedURL
 * to return things along with status codes, etc. Less than ideal: answers on a postcard, please.
 * 
 */

// XXX Add useful info to ConnectionException on way out

// XXX synchronized to handle Nuxeo race
public class ServicesConnection {
	private String base_url;
	private static HttpClient client;

	private void initClient() {
		if(client!=null)
			return;
		synchronized(getClass()) {
			if(client!=null)
				return;
			MultiThreadedHttpConnectionManager manager=new MultiThreadedHttpConnectionManager();
			client=new HttpClient(manager);
		}
	}

	public ServicesConnection(String base_url) {
		if(base_url.endsWith("/"))
			base_url=base_url.substring(0,base_url.length()-1);
		this.base_url=base_url;
		initClient();
	}

	private String prepend_base(String uri) throws ConnectionException {
		if(uri==null)
			throw new ConnectionException("URI cannot be null");
		if(!uri.startsWith("/"))
			uri="/"+uri;
		return base_url+uri;
	}

	private HttpMethod createMethod(RequestMethod method,String uri,InputStream data) throws ConnectionException {
		uri=prepend_base(uri);
		System.err.println(uri);
		if(uri==null)
			throw new ConnectionException("URI must not be null");		
		switch(method) {
		case POST: {
			PostMethod out=new PostMethod(uri);
			if(data!=null)
				out.setRequestEntity(new InputStreamRequestEntity(data));
			return out;
		}
		case PUT: {
			PutMethod out=new PutMethod(uri);
			if(data!=null)
				out.setRequestEntity(new InputStreamRequestEntity(data));
			return out;
		}
		case GET:
			return new GetMethod(uri);
		case DELETE:
			return new DeleteMethod(uri);
		}
		throw new ConnectionException("Unsupported method "+method);
	}

	private void closeStream(InputStream stream) throws ConnectionException {
		if(stream!=null)
			try {
				stream.close();
			} catch (IOException e) {
				// Close failed: nothing we can do. Is a ByteArrayInputStream, anyway, should be impossible.
				throw new ConnectionException("Impossible exception raised during close of BAIS!?");
			}		
	}

	// XXX eugh! error case control-flow nightmare
	private void doRequest(Returned out,RequestMethod method_type,String uri,RequestDataSource src) throws ConnectionException {
		InputStream body_data=null;
		if(src!=null) {
			body_data=src.getStream();
		}
		try {
			System.err.println("Getting from "+uri);
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type",src.getMIMEType());
				System.err.println("SENDING\n");
				body_data=new TeeInputStream(body_data,System.err);
			}
			try {
				int response=client.executeMethod(method);
				out.setResponse(method,response);
			} catch(Exception e) {
				throw new ConnectionException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			closeStream(body_data);
		}
	}

	private RequestDataSource makeDocumentSource(Document body) throws ConnectionException {
		RequestDataSource src=null;
		if(body!=null) {
			src=new DocumentRequestDataSource(body);
		}
		return src;
	}

	private RequestDataSource makeMultipartSource(Map<String,Document> body) throws ConnectionException {
		RequestDataSource src=null;
		if(body!=null) {
			src=new MultipartRequestDataSource(body);
		}
		return src;
	}	
	
	public ReturnedDocument getXMLDocument(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		ReturnedDocument out=new ReturnedDocument();
		doRequest(out,method_type,uri,makeDocumentSource(body));
		return out;
	}

	public ReturnedMultipartDocument getMultipartXMLDocument(RequestMethod method_type,String uri,Map<String,Document> body) throws ConnectionException {
		ReturnedMultipartDocument out=new ReturnedMultipartDocument();
		doRequest(out,method_type,uri,makeMultipartSource(body));
		return out;
	}

	public String getTextDocument(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		ReturnedText out=new ReturnedText();
		doRequest(out,method_type,uri,makeDocumentSource(body));
		return out.getText();
	}

	public ReturnedURL getURL(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		ReturnedURL out=new ReturnedURL();
		doRequest(out,method_type,uri,makeDocumentSource(body));
		out.relativize(base_url); // Annoying, but we don't want to have factories etc. or too many args
		return out;
	}

	public ReturnedURL getMultipartURL(RequestMethod method_type,String uri,Map<String,Document> body) throws ConnectionException {
		ReturnedURL out=new ReturnedURL();
		doRequest(out,method_type,uri,makeMultipartSource(body));
		out.relativize(base_url); // Annoying, but we don't want to have factories etc. or too many args
		return out;
	}

	public int getNone(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		ReturnedNone out=new ReturnedNone();
		doRequest(out,method_type,uri,makeDocumentSource(body));
		return out.getStatus();
	}
}
