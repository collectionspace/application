package org.collectionspace.csp.api.persistence;

public class InsufficientPermissions extends UnderlyingStorageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InsufficientPermissions() {
		super();
	}

	public InsufficientPermissions(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public InsufficientPermissions(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public InsufficientPermissions(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public InsufficientPermissions(String message, Integer status, String url) {
		super(message, status, url);
		// TODO Auto-generated constructor stub
	}

	public InsufficientPermissions(String message, Integer status, String url, Throwable cause) {
		super(message, status, url, cause);
		// TODO Auto-generated constructor stub
	}

}
