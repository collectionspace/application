package org.collectionspace.chain.storage.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.input.TeeInputStream;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class ServicesConnection {
	private String base_url;
	private HttpClient client;
	
	public ServicesConnection(String base_url) {
		if(base_url.endsWith("/"))
			base_url=base_url.substring(0,base_url.length()-1);
		this.base_url=base_url;
		client=new HttpClient();
	}
	
	private String prepend_base(String uri) throws BadRequestException {
		if(uri==null)
			throw new BadRequestException("URI cannot be null");
		if(!uri.startsWith("/"))
			uri="/"+uri;
		return base_url+uri;
	}
	
	private HttpMethod createMethod(RequestMethod method,String uri,InputStream data) throws BadRequestException {
		uri=prepend_base(uri);
		System.err.println(uri);
		if(uri==null)
			throw new BadRequestException("URI must not be null");		
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
		throw new BadRequestException("Unsupported method "+method);
	}
		
	public ReturnedDocument getXMLDocument(RequestMethod method,String uri) throws BadRequestException {
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

	// XXX eugh! error case control-flow nightmare
	public ReturnedDocument getXMLDocument(RequestMethod method_type,String uri,Document body) throws BadRequestException {
		InputStream body_data=null;
		if(body!=null) {
			try {
				body_data=serializetoXML(body);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			}
		}
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
				Document out=reader.read(new TeeInputStream(stream,System.err));
				System.err.println("RECEIVING "+out.asXML());
				System.err.println("ok");
				stream.close();
				return new ReturnedDocument(response,out);
			} catch (HttpException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} catch (DocumentException e) {
				throw new BadRequestException("Bad XML from "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			if(body_data!=null)
				try {
					body_data.close();
				} catch (IOException e) {
					// Close failed: nothing we can do. Is a ByteArrayInputStream, anyway, should be impossible.
					throw new BadRequestException("Impossible exception raised during close of BAIS!?");
				}
		}
	}

	// XXX refactor!!!!
	public ReturnedURL getURL(RequestMethod method_type,String uri,Document body) throws BadRequestException {
		if(body!=null) {
			System.err.println("Debug: SENDING "+body.asXML());
		}
		InputStream body_data=null;
		if(body!=null) {
			try {
				body_data=serializetoXML(body);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			}
		}
		try {
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type","application/xml");
			}
			try {
				int response=client.executeMethod(method);
				System.err.println("response="+response);
				Header location=method.getResponseHeader("Location");
				if(location==null)
					throw new BadRequestException("Missing location header");
				String url=location.getValue();
				if(url.startsWith(base_url))
					url=url.substring(base_url.length());
				return new ReturnedURL(response,url);
			} catch (HttpException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			if(body_data!=null)
				try {
					body_data.close();
				} catch (IOException e) {
					// Close failed: nothing we can do. Is a ByteArrayInputStream, anyway, should be impossible.
					throw new BadRequestException("Impossible exception raised during close of BAIS!?");
				}
		}
	}

	public int getNone(RequestMethod method_type,String uri,Document body) throws BadRequestException {
		InputStream body_data=null;
		if(body!=null) {
			try {
				body_data=serializetoXML(body);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			}
		}
		try {
			HttpMethod method=createMethod(method_type,uri,body_data);
			if(body_data!=null) {
				method.setRequestHeader("Content-Type","application/xml");
			}
			try {
				int response=client.executeMethod(method);
				return response;
			} catch (HttpException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} catch (IOException e) {
				throw new BadRequestException("Could not connect to "+uri+" at "+base_url,e);
			} finally {
				method.releaseConnection();
			}
		} finally {
			if(body_data!=null)
				try {
					body_data.close();
				} catch (IOException e) {
					// Close failed: nothing we can do. Is a ByteArrayInputStream, anyway, should be impossible.
					throw new BadRequestException("Impossible exception raised during close of BAIS!?");
				}
		}
	}
}
