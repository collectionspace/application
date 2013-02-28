/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.collectionspace.chain.util.misc.JSON;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CompositeWebUIRequestPart implements UIRequest {
	private WebUIRequest parent;
	private String[] ppath;
	private Map<String,String> params=new HashMap<String,String>();
	private Map<String,String> rparams=new HashMap<String,String>();
	private String[] rpath=null;
	private boolean secondary_redirect=false;
	private Operation op_req,op_res=Operation.READ;
	private ByteArrayOutputStream body_out=new ByteArrayOutputStream();
	private String body_in; // XXX what if it's binary?
	private String dataType;
	private String mime_type_out="";
	private boolean failure=false;
	private Exception exception;
	
	public CompositeWebUIRequestPart(WebUIRequest parent,JSONObject query) throws JSONException, UIException {
		this.parent=parent;
		String ppath=query.getString("path");
		List<String> p=new ArrayList<String>();
		for(String part : ppath.split("/")) {
			if("".equals(part))
				continue;
			p.add(part);
		}		
		this.ppath=p.toArray(new String[0]);
		JSONObject params=query.optJSONObject("params");
		if(params==null)
			params=new JSONObject();
		this.params=JSON.stringObjectToMap(params);
		op_req=getRequestedOperation(query.getString("method"));
		body_in=query.optString("body");
		dataType=query.optString("dataType");
	}
	
	private int set_status() {
		switch(op_res) {
		case CREATE:
			return 201;
		default:
			return 200;
		}
	}
	
	public int getCacheMaxAgeSeconds() {
		return 0;
	}
	public void setCacheMaxAgeSeconds(int cacheMaxAgeSeconds) {
		// Ignore this for now. Caching composite requests is not really clear.
	}
	
	public JSONObject solidify() throws JSONException, UIException {
		JSONObject out=new JSONObject();
		
		int status=200;
		try {
			if(failure) {
				// Failed
				status=400;
				if(exception!=null)
					out.put("exception",ExceptionUtils.getFullStackTrace(exception));
			} else {
				// Success
				if(rpath!=null) {
					// Redirect
					StringBuffer path=new StringBuffer();
					for(String part : rpath) {
						if("".equals(part))
							continue;
						path.append('/');
						path.append(part);
					}
					boolean first=true;
					for(Map.Entry<String,String> e : rparams.entrySet()) {
						path.append(first?'?':'&');
						first=false;
						path.append(URLEncoder.encode(e.getKey(),"UTF-8"));
						path.append('=');
						path.append(URLEncoder.encode(e.getValue(),"UTF-8"));
					}
					if(secondary_redirect)
						status=set_status();
					else
						status=303;
					out.put("redirect",path.toString());
				} else {
					status=set_status();
				}
			}
			out.put("body",body_out.toString("UTF-8"));
			if(dataType != null){
				if(dataType.equals("json")){
					out.put("body",new JSONObject(body_out.toString("UTF-8")));
				}
			}
			if(body_out!=null)
				body_out.close();
			out.put("mime",mime_type_out);
		} catch (IOException e) {
			throw new UIException("Could not send error",e);
		}
		out.put("status",status);	
		return out;
	}
	
	public Operation getRequestedOperation(String method) throws UIException {
		if("POST".equals(method))
			return Operation.CREATE;
		else if("PUT".equals(method))
			return Operation.UPDATE;
		else if("DELETE".equals(method))
			return Operation.DELETE;
		else if("GET".equals(method))
			return Operation.READ;
		return Operation.READ;
	}
	
	@Override
	public void deleteRedirectArgument(String key) throws UIException { rparams.remove(key); }

	@Override
	public JSONObject getJSONBody() throws UIException {
		try {
			String jsonString = body_in;
			if (StringUtils.isBlank(jsonString)) {
				throw new UIException("No JSON content to store");
			}
			
			// Store it
			return new JSONObject(jsonString);
		} catch (JSONException e) {
			throw new UIException("Cannot get request body, JSONException",e);
		}
	}
	public byte[] getbyteBody() throws UIException {
		return null;
	}
	public String getFileName() throws UIException{
		return "";
	}
	@Override
	public String getBody() throws UIException {

		String jsonString = body_in;
		return jsonString;
	}

	public String getContentType() throws UIException{
		return null;
	}
	@Override
	public JSONObject getPostBody() throws UIException {
		JSONObject jsondata = new JSONObject();
		String jsonString = body_in;
		try {
			if(jsonString.length()>0){
				String[] data = jsonString.split("&");
				for(String item : data){
					String[] itembits = item.split("=");
					jsondata.put(URLDecoder.decode(itembits[0],"UTF-8"), URLDecoder.decode(itembits[1],"UTF-8"));
				}
			}

		} catch (JSONException e) {
			throw new UIException("Cannot get request body, JSONException",e);
		} catch (UnsupportedEncodingException e) {
			throw new UIException("Cannot get request body, UnsupportedEncodingException",e);
		}
		return jsondata;
	}

	@Override
	public String[] getPrincipalPath() throws UIException { return ppath; }

	@Override
	public String getRedirectArgument(String key) throws UIException { return rparams.get(key); }

	@Override
	public String getRequestArgument(String key) throws UIException { return params.get(key); }
	
	@Override
	public Set<String> getAllRequestArgument() throws UIException { return params.keySet(); }

	@Override
	public Operation getRequestedOperation() throws UIException { return op_req; }

	@Override
	public UISession getSession() throws UIException { return parent.getSession(); }

	public  HttpSession getHttpSession() { return null; }


	@Override
	public TTYOutputter getTTYOutputter() throws UIException {
		return new WebTTYOutputter(new PrintWriter(body_out));
	}

	@Override
	public Boolean isJSON() throws UIException {
		try{
			new JSONObject(body_out.toString("UTF-8"));
			return true;
		}
		catch (JSONException e){
			return false;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
	}
	
	@Override
	public void sendXMLResponse(String data) throws UIException {
		mime_type_out="text/xml;charset=UTF-8";
		PrintWriter pw=new PrintWriter(body_out);
		pw.print(data);
		pw.flush();
	}
	
	public void sendUnknown(String data, String contenttype, String contentDisposition) throws UIException {

		mime_type_out=contenttype;
		PrintWriter pw=new PrintWriter(body_out);
		pw.print(data);
		pw.flush();
	}

	public void sendUnknown(byte[] data, String contenttype, String contentDisposition) throws UIException {
		mime_type_out=contenttype;
		try {
			body_out.write(data);
			body_out.flush();
		} catch (IOException e) {
			throw new UIException("Could not write data",e);
		}
	}
	
	@Override
	public void sendJSONResponse(JSONObject data) throws UIException {
		mime_type_out="text/json;charset=UTF-8";
		PrintWriter pw=new PrintWriter(body_out);
		pw.print(data.toString());
		pw.flush();
	}

	@Override
	public void sendJSONResponse(JSONArray data) throws UIException {
		mime_type_out="text/json;charset=UTF-8";
		PrintWriter pw=new PrintWriter(body_out);
		pw.print(data.toString());
		pw.flush();
	}

	@Override
	public void setFailure(boolean isit, Exception why) throws UIException {
		failure=isit;
		exception=why;
	}

	@Override
	public void setOperationPerformed(Operation op) throws UIException { op_res=op; }

	@Override
	public void setRedirectArgument(String key, String value) throws UIException {
		rparams.put(key,value);
	}

	@Override
	public void setRedirectPath(String[] in) throws UIException { rpath=in; }

	@Override
	public void setSecondaryRedirectPath(String[] in) throws UIException {
		rpath=in;
		secondary_redirect=true;
	}

	@Override
	public void sendURLReponse(String url) throws UIException {
		PrintWriter pw=new PrintWriter(body_out);
		pw.print(url);
		pw.flush();
	}
}
