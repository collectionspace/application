package org.collectionspace.chain.storage;

/** JSON Not Found in store at this path */
public class ExistException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;

	public ExistException() {}
	public ExistException(String message) { super(message); }
	public ExistException(Throwable cause) { super(cause); }
	public ExistException(String message, Throwable cause) { super(message, cause); }
}
