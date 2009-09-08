/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config.api;

/** an error occurred during loading */
public class ConfigLoadFailedException extends Exception {
	public ConfigLoadFailedException() { super();  }
	public ConfigLoadFailedException(String message) { super(message); }
	public ConfigLoadFailedException(Throwable cause) { super(cause); }
	public ConfigLoadFailedException(String message, Throwable cause) { super(message, cause); }
}
