/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.persistence;

/** Storage method through an exception. Exceptions are a mess and need to be tidied. */
public class UnderlyingStorageException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;
	Integer status;
	String url;

	public UnderlyingStorageException() {}
	public UnderlyingStorageException(String message) { super(message); }
	public UnderlyingStorageException(Throwable cause) { super(cause); }
	public UnderlyingStorageException(String message, Throwable cause) { super(message, cause); }

	public UnderlyingStorageException(String message, Integer status, String url) {
		super(message);
		this.url = url;
		this.status = status;
	}
	public UnderlyingStorageException(String message, Integer status, String url, Throwable cause) {
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
	
}
