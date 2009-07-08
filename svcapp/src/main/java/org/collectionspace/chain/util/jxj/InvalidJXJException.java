/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jxj;

/** Something went wrong during transformation.
 * 
 */
public class InvalidJXJException extends Exception {
	private static final long serialVersionUID = -2311375917873439796L;

	public InvalidJXJException() { super(); }
	public InvalidJXJException(String message) { super(message); }
	public InvalidJXJException(Throwable cause) { super(cause); }
	public InvalidJXJException(String message, Throwable cause) { super(message, cause); }
}
