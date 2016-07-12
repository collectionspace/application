package org.collectionspace.csp.api.persistence;

public class ConflictException extends UnderlyingStorageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConflictException() {
		// TODO Auto-generated constructor stub
	}

	public ConflictException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public ConflictException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public ConflictException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public ConflictException(String message, Integer status, String url) {
		super(message, status, url);
		// TODO Auto-generated constructor stub
	}

	public ConflictException(String message, Integer status, String url, Throwable cause) {
		super(message, status, url, cause);
		// TODO Auto-generated constructor stub
	}

}
