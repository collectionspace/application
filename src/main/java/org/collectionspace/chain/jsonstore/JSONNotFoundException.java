package org.collectionspace.chain.jsonstore;

/** JSON Not Found in store at this path */
public class JSONNotFoundException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;

	public JSONNotFoundException() {}
	public JSONNotFoundException(String message) { super(message); }
	public JSONNotFoundException(Throwable cause) { super(cause); }
	public JSONNotFoundException(String message, Throwable cause) { super(message, cause); }
}
