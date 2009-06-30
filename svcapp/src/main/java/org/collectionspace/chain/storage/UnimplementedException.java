package org.collectionspace.chain.storage;

/** JSON Not Found in store at this path */
public class UnimplementedException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;

	public UnimplementedException() {}
	public UnimplementedException(String message) { super(message); }
	public UnimplementedException(Throwable cause) { super(cause); }
	public UnimplementedException(String message, Throwable cause) { super(message, cause); }
}
