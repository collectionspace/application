package org.collectionspace.csp.api.core;

public class CSPDependencyException extends Exception {
	private static final long serialVersionUID = 4871981011322436412L;
	private String[] cause;
	
	public CSPDependencyException() { super(); }
	public CSPDependencyException(String m) { super(m); }
	public CSPDependencyException(Exception x) { super(x); }
	public CSPDependencyException(String m,Exception x) { super(m,x); }
	
	public CSPDependencyException(String[] cause) { super(); this.cause=cause; }
	public CSPDependencyException(String m,String[] cause) { super(m); this.cause=cause; }
	public CSPDependencyException(Exception x,String[] cause) { super(x); this.cause=cause; }
	public CSPDependencyException(String m,Exception x,String[] cause) { super(m,x); this.cause=cause; }
	
	public String[] getCauses() { return cause; }
}
