/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.xtmpl;

/** Something went wrong at compile time or runtime.
 * 
 */
public class InvalidXTmplException extends Exception {
	private static final long serialVersionUID = -9033938559839598685L;

	public InvalidXTmplException() { super(); }

	public InvalidXTmplException(String message) { super(message); }

	public InvalidXTmplException(Throwable cause) { super(cause); }

	public InvalidXTmplException(String message, Throwable cause) { super(message, cause); }
}
