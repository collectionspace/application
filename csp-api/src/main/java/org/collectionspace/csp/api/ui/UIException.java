/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.ui;

/** Storage method through an exception. Exceptions are a mess and need to be tidied. */
public class UIException extends Exception {
	private static final long serialVersionUID = 7415760251413302328L;

	public UIException() {}
	public UIException(String message) { super(message); }
	public UIException(Throwable cause) { super(cause); }
	public UIException(String message, Throwable cause) { super(message, cause); }
}
