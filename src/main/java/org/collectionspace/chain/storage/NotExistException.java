package org.collectionspace.chain.storage;

/** JSON Not Found in store at this path */
public class NotExistException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;

	public NotExistException() {}
	public NotExistException(String message) { super(message); }
	public NotExistException(Throwable cause) { super(cause); }
	public NotExistException(String message, Throwable cause) { super(message, cause); }
}
