/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.util.BadRequestException;

// XXX test reset

/** This is an abstraction of the request and response which translates things expressed in web-language -
 *  servlets, request parameters, pathinfo, etc - into ours - request type, operation, etc.
 *  
 *  At the moment our language is quite close, so the implementation is quite transparent, but as APPUI develops,
 *  the distance will increase.
 * 
 */
public class ChainRequest {
	private static final String RESET_REF = "/reset";
	private static final String LOGIN_REF = "/login";
	
	private static final String usage="You must structure the requests like so: \n" +
		"GET {record-type}/%path-to-file-with-name% \n" +
		"GET {record-type}/%path-to-file-with-name% \n" +
		"POST {record-type}/%path-to-file-with-name% - note that data in body must be JSON \n";

	private HttpServletRequest req;
	private HttpServletResponse res;
	private boolean is_get;
	private RequestType type;
	private String rest,record_type,record_type_url,body=null;
	private boolean create_not_overwrite=false,found=false;
	
	private static final Map<String,String> url_to_type=new HashMap<String,String>();
	
	static {
		url_to_type.put("objects","collection-object");
		url_to_type.put("intake","intake");
		url_to_type.put("acquisition","acquisition");
	}
	
	private void perhapsStartsWith(String what,RequestType rq,String path,String record,String record_url) throws BadRequestException {
		if(!path.startsWith(what))
			return; // Nope, it doesn't
		// Yes it does
		type=rq;
		rest=path.substring(what.length());
		if(record!=null) {
			this.record_type=record;
			this.record_type_url=record_url;
		}
		if(rest.startsWith("/"))
			rest=rest.substring(1);
		// Capture body
		if(!is_get) {
			try {
				body=IOUtils.toString(req.getReader());
			} catch (IOException e) {
				throw new BadRequestException("Cannot capture request body");
			}
		}
		found=true;
	}
	
	/** Wrapper for requests for chain
	 * 
	 * @param req the servlet request
	 * @param res the servlet response
	 * @throws BadRequestException cannot build valid chain request from servlet request
	 */
	public ChainRequest(HttpServletRequest req,HttpServletResponse res) throws BadRequestException {
		this.req=req;
		this.res=res;
		String path = req.getPathInfo();
		// Individual record types
		for(Map.Entry<String,String> e : url_to_type.entrySet()) {
			if(found)
				break;
			perhapsStartsWith("/"+e.getKey()+"/schema",RequestType.SCHEMA,path,e.getValue(),e.getKey());
		}
		for(Map.Entry<String,String> e : url_to_type.entrySet()) {
			if(found)
				break;
			perhapsStartsWith("/"+e.getKey()+"/__auto",RequestType.AUTO,path,e.getValue(),e.getKey());
		}
		for(Map.Entry<String,String> e : url_to_type.entrySet()) {
			if(found)
				break;
			perhapsStartsWith("/"+e.getKey(),RequestType.STORE,path,e.getValue(),e.getKey());
		}	
		// Regular URLs
		if(!found)
			perhapsStartsWith(RESET_REF,RequestType.RESET,path,null,null);
		if(!found)
			perhapsStartsWith(LOGIN_REF,RequestType.LOGIN,path,null,null);
		if(type==RequestType.STORE && "".equals(rest)) {
			// Blank means list
			type=RequestType.LIST;
		}
		String method=req.getMethod();
		// Allow method to be overridden by params for testing
		is_get="GET".equals(method);
		if("POST".equals(method)) {
			create_not_overwrite=true;
		}
		// Mmm. Perhaps it's a non-get request with stuff in parameters.
		if(!found)
			throw new BadRequestException("Invalid path "+path);
		if(path==null || "".equals(path))
			throw new BadRequestException(usage);
	}
	
	/** What overall type is the request? ie controller selection.
	 * 
	 * @return the type
	 */
	public RequestType getType() { return type; }
	
	/** What's the trailing path of the request?
	 * 
	 * @return the trailing path, ie after controller selection.
	 */
	public String getPathTail() { return rest; }

	/** What's the request body? Either the real body, or the fake one from the query parameter.
	 * 
	 * @return the body
	 */
	public String getBody() { return body; }
	
	/** Returns a printwriter for some JSON, having set up mime-type, etc, correctly.
	 * 
	 * @return
	 * @throws IOException 
	 */
	public PrintWriter getJSONWriter() throws IOException {
		// Set response type to JSON
		res.setCharacterEncoding("UTF-8");
		res.setContentType("application/json");
		// Return JSON
		
		return res.getWriter();
	}
	
	/** Method/params indicate data should be created at path, not updated
	 * 
	 * @return
	 */
	public boolean isCreateNotOverwrite() {
		return create_not_overwrite;
	}
	
	public void setStatus(int status) {
		res.setStatus(status);
	}
	
	public void setContentType(String mime) {
		res.setContentType(mime);
	}

	public void redirect(String path) {
		res.addHeader("Location",path);
	}
	
	public String getRecordType() { return record_type; }
	public String getRecordTypeURL() { return record_type_url; }

	public String xxxGetUsername() { return req.getParameter("userid"); }
	public String xxxGetPassword() { return req.getParameter("password"); }
}
