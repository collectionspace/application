package org.collectionspace.csp.api.persistence;

public class UnauthorizedException extends UnderlyingStorageException {

	private static final long serialVersionUID = 1L;

	public UnauthorizedException() {
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message, Integer status, String url) {
		super(message, status, url);
		// TODO Auto-generated constructor stub
	}

	public UnauthorizedException(String message, Integer status, String url, Throwable cause) {
		super(message, status, url, cause);
		// TODO Auto-generated constructor stub
	}

}
