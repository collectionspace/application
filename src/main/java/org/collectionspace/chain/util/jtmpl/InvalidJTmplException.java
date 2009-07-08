/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jtmpl;

/** Something went wrong, at compile time or runtime, with your template.
 * 
 */
public class InvalidJTmplException extends Exception {
	private static final long serialVersionUID = 4708546839564877741L;

	public InvalidJTmplException() { super(); }
	public InvalidJTmplException(String message) { super(message); }
	public InvalidJTmplException(Throwable cause) { super(cause); }
	public InvalidJTmplException(String message, Throwable cause) { super(message, cause); }
}
