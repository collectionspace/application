package org.collectionspace.chain.storage;

/** JSON Not Found in store at this path */
public class UnderlyingStorageException extends Exception {
	private static final long serialVersionUID = 1757045769877257914L;

	public UnderlyingStorageException() {}
	public UnderlyingStorageException(String message) { super(message); }
	public UnderlyingStorageException(Throwable cause) { super(cause); }
	public UnderlyingStorageException(String message, Throwable cause) { super(message, cause); }
}
