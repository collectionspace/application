/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jpath;

/** Used by JPath at compile or run time when path is invalid for some reason.
 * 
 */
public class InvalidJPathException extends Exception {
	private static final long serialVersionUID = 5102348119739177775L;

	public InvalidJPathException() {
		super();
	}

	public InvalidJPathException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidJPathException(String message) {
		super(message);
	}

	public InvalidJPathException(Throwable cause) {
		super(cause);
	}
}
