/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StreamUIRequest implements UIRequest {
	private LineIterator in;
	private OutputStream out,err;
	private InputStream ibody;
	private String[] path,rpath;
	private boolean secondary_redirect=false;
	private Operation request;
	private Map<String,String> rargs=new HashMap<String,String>();
	private Map<String,String> qargs=new HashMap<String,String>();

	public StreamUIRequest(InputStream in,OutputStream out,OutputStream err,Operation request,String[] path,Map<String,String> args) throws IOException {
		this.ibody=in;
		this.in=IOUtils.lineIterator(in,"UTF-8");
		this.out=out;
		this.err=err;
		this.path=path;
		this.request=request;
		if(args!=null) {
			qargs=new HashMap<String,String>(args);
		}
	}

	private static void print(OutputStream stream,String x) throws UIException {
		try {
			stream.write(x.getBytes("UTF-8"));
			stream.flush();
		} catch (IOException e) {
			throw new UIException("IOException writing line",e);
		}
	}

	private static void println(OutputStream stream,String x) throws UIException {
		print(stream,x+"\n");
	}

	public TTYOutputter getTTYOutputter() {
		return new TTYOutputter(){
			public void flush() throws UIException { 
				try {
					out.flush();
				} catch (IOException e) {
					throw new UIException("IOException",e);
				}
			}

			public void line(String text) throws UIException {
				println(out,text);
			}
		};
	}

	public Object getUnderlyingObject(String name) throws UIException { throw new UIException("No underlying object"); }

	public String[] getPrincipalPath() throws UIException { return path; }

	private String exception_to_text(Throwable e) {
		StringBuffer buf=new StringBuffer();
		exception_to_text_internal(buf,e);
		return buf.toString();
	}

	private void exception_to_text_internal(StringBuffer buf,Throwable e) {
		buf.append("Exception "+e.getClass()+" thrown message=\""+e.getMessage()+"\"\n");
		for(StackTraceElement el : e.getStackTrace()) {
			buf.append(el.getClassName()+" "+el.getMethodName()+" ("+el.getFileName()+":"+el.getLineNumber()+")\n");
		}
		Throwable next=e.getCause();
		if(next!=null && next!=e) {
			buf.append("Caused by:\n");
			exception_to_text_internal(buf,next);
		}
	}

	public int getCacheMaxAgeSeconds() {
		return 0;
	}
	public void setCacheMaxAgeSeconds(int cacheMaxAgeSeconds) {
		// Ignore this for now. Need to figure out how to set cache for things like report outputs (probably never)
		// and image blobs (perhaps aggressively?).
	}

	public void setFailure(boolean isit, Exception why) throws UIException {
		if(!isit) {
			/* Not a failure */
			println(err,"Operation was a success!");
			return;
		}
		println(err,"Operation failed: "+((why==null)?"no exception noted":"exception given below"));		
		if(why!=null) {
			print(err,exception_to_text(why));
		}
	}

	// XXX summarize
	public void setOperationPerformed(Operation op) throws UIException {
		println(err,"Operation performed was : "+op.toString());
	}

	public void deleteRedirectArgument(String key) throws UIException {
		rargs.remove(key);
	}

	public String getRedirectArgument(String key) throws UIException {
		return rargs.get(key);
	}

	public void setRedirectArgument(String key, String value) throws UIException {
		rargs.put(key,value);
	}

	public void setRedirectPath(String[] in) throws UIException {
		rpath=in;
		secondary_redirect=false;
	}

	public String getRequestArgument(String key) throws UIException {
		return qargs.get(key);
	}
	public Set<String> getAllRequestArgument() throws UIException {
		return qargs.keySet();
	}

	public void sendXMLResponse(String data) throws UIException {
		println(out,data);
	}
	public void sendUnknown(String data, String contenttype, String contentDisposition) throws UIException {
		println(out,data);
	}
	
	public void sendUnknown(byte[] data, String contenttype, String contentDisposition) throws UIException {
		try {
			out.write(data);
		} catch (IOException e) {
			throw new UIException("Cannot write data",e);
		}
	}

	
	public void sendJSONResponse(JSONObject data) throws UIException {
		println(out,data.toString());
	}
	public void sendJSONResponse(JSONArray data) throws UIException {
		println(out,data.toString());
	}

	public Operation getRequestedOperation() throws UIException { return request; }

	public JSONObject getJSONBody() throws UIException {
		try {
			if(in.hasNext())
				return new JSONObject(in.nextLine());
			else
				return null;
		} catch (JSONException e) {
			throw new UIException("Bad JSON on standard input",e);
		}
	}
	public String getBody() throws UIException {

		if(in.hasNext())
			return in.nextLine();
		return null;
	}
	public String getContentType() throws UIException{
		return null;
	}
	public byte[] getbyteBody() throws UIException {
		return null;
	}
	public String getFileName() throws UIException{
		return "";
	}
	public JSONObject getPostBody() throws UIException {
		try {
			if(in.hasNext())
				return new JSONObject(in.nextLine());
			else
				return null;
		} catch (JSONException e) {
			throw new UIException("Bad JSON on standard input",e);
		}
	}
	public Boolean isJSON(){
		
		return true;
	}

	public void setSecondaryRedirectPath(String[] in) throws UIException {
		rpath=in;
		secondary_redirect=true;
	}

	public UISession getSession() { return null; } // XXX support this?
	public  HttpSession getHttpSession() { return null; }

	public void sendURLReponse(String url) throws UIException {
		println(out, url);
	}

}
