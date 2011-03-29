/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config;

/** Storage method through an exception. Exceptions are a mess and need to be tidied. */
public class ConfigException extends Exception {
	private static final long serialVersionUID = 2196051097424214385L;

	public ConfigException() {}
	public ConfigException(String message) { super(message); }
	public ConfigException(Throwable cause) { super(cause); }
	public ConfigException(String message, Throwable cause) { super(message, cause); }
}
