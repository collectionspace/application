/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.persistence;

/** Already exists for create. Exceptions are a mess, and should be tidied. */
public class ExistException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;
	private int status = 0;

	public ExistException() {
	}

	public ExistException(String message) {
		super(message);
	}

	public ExistException(String message, int status) {
		super(message);
		this.status = status;
	}

	public ExistException(Throwable cause) {
		super(cause);
	}

	public ExistException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ExistException(String message, Throwable cause, int status) {
		super(message, cause);
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
}
