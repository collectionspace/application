/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.nconfig.impl.main;

/** Storage method through an exception. Exceptions are a mess and need to be tidied. */
public class NConfigException extends Exception {
	private static final long serialVersionUID = 2196051097424214385L;

	public NConfigException() {}
	public NConfigException(String message) { super(message); }
	public NConfigException(Throwable cause) { super(cause); }
	public NConfigException(String message, Throwable cause) { super(message, cause); }
}
