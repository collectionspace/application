/* Copyright 2010 University of Cambridge
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
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.input.TeeInputStream;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The actual REST calls are handled by ServicesConnection, which uses utility types ReturnedDocument and ReturnedURL
 * to return things along with status codes, etc. Less than ideal: answers on a postcard, please.
 * 
 */

// XXX Add useful info to ConnectionException on way out

public class ServicesConnection {
	private static final Logger log=LoggerFactory.getLogger(ServicesConnection.class);
	private static final Logger perflog=LoggerFactory.getLogger("org.collectionspace.perflog");
	private String base_url,ims_url;
	private MultiThreadedHttpConnectionManager manager;
	
	private void initClient() {
		if(manager!=null)
			return;
		synchronized(getClass()) {
			if(manager!=null)
				return;
			manager=new MultiThreadedHttpConnectionManager();
		}
	}

	public HttpClient makeClient(CSPRequestCredentials creds,CSPRequestCache cache) {
		// Check request cache
		HttpClient client=(HttpClient)cache.getCached(getClass(),new String[]{"client"});
		if(client!=null)
			return client;
		client=new HttpClient(manager);
		client.getState().setCredentials(
				new AuthScope(AuthScope.ANY_HOST,AuthScope.ANY_PORT,AuthScope.ANY_REALM),
				new UsernamePasswordCredentials((String)creds.getCredential(ServicesStorageGenerator.CRED_USERID),
												(String)creds.getCredential(ServicesStorageGenerator.CRED_PASSWORD)));
		cache.setCached(getClass(),new String[]{"client"},client);
		return client;
	}
	
	public ServicesConnection(String base_url,String ims_url) {
		if(base_url.endsWith("/"))
			base_url=base_url.substring(0,base_url.length()-1);
		this.base_url=base_url;
		this.ims_url=ims_url;
		initClient();
	}

	public String getBase(){
		return base_url;
	}
	public String getIMSBase(){
		return ims_url;
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
		if(uri==null)
			throw new ConnectionException("URI must not be null");		
		// Extract QP's
		int qp_start=uri.indexOf('?');
		String qps=null;
		if(qp_start!=-1) {
			qps=uri.substring(qp_start+1);
			uri=uri.substring(0,qp_start);
		}
		HttpMethod out=null;
		switch(method) {
		case POST: {
			out=new PostMethod(uri);
			if(data!=null)
				((PostMethod)out).setRequestEntity(new InputStreamRequestEntity(data));
			break;
		}
		case PUT: {
			out=new PutMethod(uri);
			if(data!=null)
				((PutMethod)out).setRequestEntity(new InputStreamRequestEntity(data));
			break;
		}
		case GET:
			out=new GetMethod(uri);
			break;
		case DELETE:
			out=new DeleteMethod(uri);
			break;
		default:
			throw new ConnectionException("Unsupported method "+method,0,uri);
		}
		if(qps!=null)
			out.setQueryString(qps);
		out.setDoAuthentication(true);
		return out;
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
	private void doRequest(Returned out,RequestMethod method_type,String uri,RequestDataSource src,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		InputStream body_data=null;
		if(src!=null) {
			body_data=src.getStream();
		}
		try {
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type",src.getMIMEType());
				// XXX Not sure if or when this ever actually writes to stderr?
				body_data=new TeeInputStream(body_data,System.err);
			}
			try {
				HttpClient client=makeClient(creds,cache);

				String requestContext = null;
				if(perflog.isDebugEnabled()) {
					// TODO add more context, e.g. session id?
					requestContext  = "HttpClient@" + Integer.toHexString(client.hashCode());
					requestContext += "/CSPRequestCache@" + Integer.toHexString(cache.hashCode()) + ",";
					//String queryString = method.getQueryString();
					perflog.debug(System.currentTimeMillis()+",\""+Thread.currentThread().getName()+"\",app,svc," + requestContext
							+ method.getName() + " " + method.getURI()
							//+ (queryString!=null ? queryString : "")
									);
				}

				int response=client.executeMethod(method);

				if(perflog.isDebugEnabled()) {
					perflog.debug(System.currentTimeMillis()+",\""+Thread.currentThread().getName()+"\",svc,app," + requestContext + "HttpClient.executeMethod done");
				}

				out.setResponse(method,response);
			} catch(ConnectionException e) {
				throw new ConnectionException(e.getMessage(),e.status,base_url+"/"+uri,e);
			} catch (Exception e) {
				throw new ConnectionException(e.getMessage(),0,base_url+"/"+uri,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			closeStream(body_data);
		}
	}

	private RequestDataSource makeStringSource(byte[] body, String type,String uploadname) throws ConnectionException {
		RequestDataSource src=null;
		if(body!=null) {
			src=new StringRequestDataSource(body, type, uploadname);
		}
		return src;
	}
	private RequestDataSource makeDocumentSource(Document body) throws ConnectionException {
		RequestDataSource src=null;
		if(body!=null) {
			src=new DocumentRequestDataSource(body);
		}
		return src;
	}

	private RequestDataSource makeUnknownSource(Document body) throws ConnectionException {
		RequestDataSource src=null;
		if(body!=null) {
			src=new DocumentUnknownRequestDataSource(body);
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
	
	public ReturnedDocument getXMLDocument(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedDocument out=new ReturnedDocument();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		return out;
	}

	public ReturnUnknown getUnknownDocument(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnUnknown out=new ReturnUnknown();
		doRequest(out,method_type,uri,makeUnknownSource(body),creds,cache);
		return out;
	}

	public ReturnUnknown getReportDocument(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnUnknown out=new ReturnUnknown();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		return out;
	}

	public ReturnedDocument getBatchDocument(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedDocument out=new ReturnedDocument();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		return out;
	}
	
	public ReturnedMultipartDocument getMultipartXMLDocument(RequestMethod method_type,String uri,Map<String,Document> body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedMultipartDocument out=new ReturnedMultipartDocument();
		doRequest(out,method_type,uri,makeMultipartSource(body),creds,cache);
		return out;
	}

	public String getTextDocument(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedText out=new ReturnedText();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		return out.getText();
	}

	public ReturnedURL getStringURL(RequestMethod method_type,String uri,byte[] body,String uploadname,String type,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedURL out=new ReturnedURL();
		doRequest(out,method_type,uri,makeStringSource(body,type,uploadname),creds,cache);
		out.relativize(base_url); // Annoying, but we don't want to have factories etc. or too many args
		return out;
	}
	
	public ReturnedURL getURL(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedURL out=new ReturnedURL();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		out.relativize(base_url); // Annoying, but we don't want to have factories etc. or too many args
		return out;
	}

	public ReturnedURL getMultipartURL(RequestMethod method_type,String uri,Map<String,Document> body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedURL out=new ReturnedURL();
		doRequest(out,method_type,uri,makeMultipartSource(body),creds,cache);
		out.relativize(base_url); // Annoying, but we don't want to have factories etc. or too many args
		return out;
	}

	public int getNone(RequestMethod method_type,String uri,Document body,CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException {
		ReturnedNone out=new ReturnedNone();
		doRequest(out,method_type,uri,makeDocumentSource(body),creds,cache);
		return out.getStatus();
	}
}
