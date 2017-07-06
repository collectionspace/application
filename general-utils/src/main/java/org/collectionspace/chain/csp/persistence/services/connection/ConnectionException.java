/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

/** Used internally in the controller. Exceptions are going to be refactored shortly. This one is doomed.
 * 
 */
public class ConnectionException extends Exception {
	private static final long serialVersionUID = -129549065899185645L;
	Integer status;
	String url;

	public ConnectionException() {
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(Throwable cause) {
		super(cause);
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ConnectionException(String message, Integer status, String url) {
		super(message);
		this.url = url;
		this.status = status;
	}
	public ConnectionException(String message, int status, String url, Throwable cause) {
		super(message, cause);
		this.url = url;
		this.status = status;
	}
	
	public String getUrl(){
		return this.url;
	}
	
	public Integer getStatus(){
		return this.status;
	}
	/*
	public String getMessage(){
		String parent_msg = super.getMessage();
		String msg = "";
		if(this.url!=null){
			msg += "URL:"+this.url+":";
		}
		if(this.status!=null){

			msg += "STATUS:"+Integer.toString(this.status)+":";
		}
		msg += parent_msg;
		return msg;
	}
	*/
}
