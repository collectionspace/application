/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

/** an error occurred during loading */
public class BootstrapConfigLoadFailedException extends Exception {
	public BootstrapConfigLoadFailedException() { super();  }
	public BootstrapConfigLoadFailedException(String message) { super(message); }
	public BootstrapConfigLoadFailedException(Throwable cause) { super(cause); }
	public BootstrapConfigLoadFailedException(String message, Throwable cause) { super(message, cause); }
}
