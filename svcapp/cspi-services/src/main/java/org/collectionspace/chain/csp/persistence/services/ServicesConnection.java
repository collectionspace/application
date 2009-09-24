/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/** The actual REST calls are handled by ServicesConnection, which uses utility types ReturnedDocument and ReturnedURL
 * to return things along with status codes, etc. Less than ideal: answers on a postcard, please.
 * 
 */
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
		
	public ReturnedDocument getXMLDocument(RequestMethod method,String uri) throws ConnectionException {
		return getXMLDocument(method,uri,null);
	}
	
	public InputStream serializetoXML(Document doc) throws IOException {
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");
		XMLWriter writer = new XMLWriter(out, outformat);
		writer.write(doc);
		writer.flush();
		out.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private InputStream documentToStream(Document in,String uri) throws ConnectionException {
		if(in!=null) {
			try {
				return serializetoXML(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect to "+uri+" at "+base_url,e);
			}
		}
		return null;
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
	public ReturnedDocument getXMLDocument(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		InputStream body_data=documentToStream(body,uri);
		try {
			System.err.println("Getting from "+uri);
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type","application/xml");
			}
			try {
				int response=client.executeMethod(method);
				System.err.println("response="+response);
				InputStream stream=method.getResponseBodyAsStream();
				SAXReader reader=new SAXReader();
				// TODO errorhandling
				Document out=null;
				Header content_type=method.getResponseHeader("Content-Type");
				if(content_type!=null && "application/xml".equals(content_type.getValue())) {
					out=reader.read(new TeeInputStream(stream,System.err));
					System.err.println("RECEIVING "+out.asXML());
				}
				System.err.println("ok");
				stream.close();
				return new ReturnedDocument(response,out);
			} catch(Exception e) {
				throw new ConnectionException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			closeStream(body_data);
		}
	}

	// XXX refactor!!!!
	public ReturnedURL getURL(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		InputStream body_data=documentToStream(body,uri);
		try {
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type","application/xml");
			}
			try {
				int response=client.executeMethod(method);
				System.err.println("response="+response);
				System.err.println("response="+(method.getResponseBodyAsString()));
				Header location=method.getResponseHeader("Location");
				if(location==null)
					throw new ConnectionException("Missing location header");
				String url=location.getValue();
				if(url.startsWith(base_url))
					url=url.substring(base_url.length());
				return new ReturnedURL(response,url);
			} catch (Exception e) {
				throw new ConnectionException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			closeStream(body_data);
		}
	}

	public int getNone(RequestMethod method_type,String uri,Document body) throws ConnectionException {
		InputStream body_data=documentToStream(body,uri);
		try {
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type","application/xml");
			}
			try {
				int response=client.executeMethod(method);
				return response;
			} catch (Exception e) {
				throw new ConnectionException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			closeStream(body_data);
		}
	}
}
