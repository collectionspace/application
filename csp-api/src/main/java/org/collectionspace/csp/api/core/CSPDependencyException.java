/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.api.core;

public class CSPDependencyException extends Exception {
	private static final long serialVersionUID = 4871981011322436412L;
	private CSPDependencyException[] cause;
	
	public CSPDependencyException() { super(); }
	public CSPDependencyException(String m) { super(m); }
	public CSPDependencyException(Exception x) { super(x); }
	public CSPDependencyException(String m,Exception x) { super(m,x); }
	
	public CSPDependencyException(CSPDependencyException[] cause) { super(); this.cause=cause; }
	public CSPDependencyException(String m,CSPDependencyException[] cause) { super(m); this.cause=cause; }
	public CSPDependencyException(Exception x,CSPDependencyException[] cause) { super(x); this.cause=cause; }
	public CSPDependencyException(String m,Exception x,CSPDependencyException[] cause) { super(m,x); this.cause=cause; }
	
	public CSPDependencyException[] getCauses() { return cause; }
}
