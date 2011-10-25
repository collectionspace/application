/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Storage method through an exception. Exceptions are a mess and need to be tidied. */
public class UIException extends Exception {
	private static final long serialVersionUID = 7415760251413302328L;
	Integer status;
	String url;

	public UIException() {}
	public UIException(String message) { super(message); }
	public UIException(Throwable cause) { super(cause); }
	public UIException(String message, Throwable cause) { super(message, cause); }
	public UIException(String message, Integer status, String url) {
		super(message);
		this.url = url;
		this.status = status;
	}
	public UIException(String message, Integer status, String url, Throwable cause) {
		super(message, cause);
		this.url = url;
		this.status = status;
	}
	public String getPrettyMessage(){
		String parent_msg = super.getMessage();
		String msg = "";
		if(this.url!=null){
			//don't show url at the moment
			//msg += " URL:"+this.url+":";
		}
		if(this.status!=null){
//don't show status at the moment
			//msg += " STATUS:"+Integer.toString(this.status)+":";
		}
		msg += parent_msg;
		return msg;
	}
	public String getUrl(){
		return this.url;
	}
	
	public Integer getStatus(){
		return this.status;
	}
	
	public JSONObject getJSON(){
		try {
			JSONObject error = new JSONObject();
			JSONArray arr = new JSONArray();
			JSONObject messages = new JSONObject();
			messages.put("severity", "error");
			messages.put("message", this.getPrettyMessage());
			//error.put("status", this.getStatus());
			arr.put(messages);
			error.put("isError", true);
			error.put("messages", arr);
			//error.put("url", this.getUrl());
			//error.put("messages", this.getPrettyMessage());
			//error.put("stack", this.getStackTrace());
			return error;
		} catch (JSONException e) {
			// well if the JSON fails we have bad thinsg all around.
			return  new JSONObject();
		}
	}
}
