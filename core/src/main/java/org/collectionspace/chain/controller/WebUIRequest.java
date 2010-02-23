package org.collectionspace.chain.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class WebUIRequest implements UIRequest {
	private HttpServletRequest request;
	private HttpServletResponse response;
	private String[] ppath,rpath=null;
	private boolean failure=false,secondary_redirect=false;
	private Exception failing_exception;
	private Operation operation_performed=Operation.READ;
	private Map<String,String> rargs=new HashMap<String,String>();
	private PrintWriter out=null;
	private String body;

	public WebUIRequest(HttpServletRequest request,HttpServletResponse response) throws IOException {
		this.request=request;
		this.response=response;
		List<String> p=new ArrayList<String>();
		for(String part : request.getPathInfo().split("/")) {
			if("".equals(part))
				continue;
			p.add(part);
		}		
		this.ppath=p.toArray(new String[0]);
		body=IOUtils.toString(request.getInputStream(),"UTF-8");
	}

	public TTYOutputter getTTYOutputter() throws UIException { 
		try {
			WebTTYOutputter tty=new WebTTYOutputter(response);
			out=tty.getWriter();
			return tty;
		} catch (IOException e) {
			throw new UIException("Cannot create response PrintWriter");
		}
	}

	public Object getUnderlyingObject(String name) throws UIException {
		if("servlet.http-request".equals(name))
			return request;
		if("servlet.http-response".equals(name))
			return response;
		throw new UIException("No such object");
	}

	public String[] getPrincipalPath() throws UIException { return ppath; }

	public void setFailure(boolean isit, Exception why) throws UIException {
		failure=isit;
		failing_exception=why;
	}

	public void setOperationPerformed(Operation op) throws UIException {
		operation_performed=op;
	}

	public void setRedirectPath(String[] in) throws UIException {
		rpath=in;
		secondary_redirect=false;
	}

	public void setSecondaryRedirectPath(String[] in) throws UIException {
		rpath=in;
		secondary_redirect=true;
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

	public String getRequestArgument(String key) throws UIException {
		return request.getParameter(key);
	}

	public Operation getRequestedOperation() throws UIException {
		String method=request.getMethod();
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
	
	private void set_status() {
		switch(operation_performed) {
		case CREATE:
			response.setStatus(201);
			break;
		default:
			response.setStatus(200);
		}
	}
	
	public void solidify() throws UIException {
		try {
			if(failure) {
				// Failed
				response.setStatus(400);
				if(failing_exception!=null)
					response.sendError(400,ExceptionUtils.getFullStackTrace(failing_exception));
				else
					response.sendError(400,"No underlying exception");
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
					for(Map.Entry<String,String> e : rargs.entrySet()) {
						path.append(first?'?':'&');
						first=false;
						path.append(URLEncoder.encode(e.getKey(),"UTF-8"));
						path.append('=');
						path.append(URLEncoder.encode(e.getValue(),"UTF-8"));
					}
					if(secondary_redirect)
						set_status();
					else
						response.setStatus(303);
					response.setHeader("Location",path.toString());
				} else {
					set_status();
				}
			}
			if(out!=null)
				out.close();
		} catch (IOException e) {
			throw new UIException("Could not send error",e);
		}
	}

	public void sendJSONResponse(JSONObject data) throws UIException {
		try {
			response.setContentType("text/json;charset=UTF-8");
			out=response.getWriter();
			out.print(data.toString());
		} catch (IOException e) {
			throw new UIException("Cannot send JSON to client",e);
		}
	}

	public JSONObject getJSONBody() throws UIException {
		try {
			String jsonString = body;
			if (StringUtils.isBlank(jsonString)) {
				throw new UIException("No JSON content to store");
			}
			// Store it
			return new JSONObject(jsonString);
		} catch (JSONException e) {
			throw new UIException("Cannot get request body, JSONException",e);
		}
	}
}
