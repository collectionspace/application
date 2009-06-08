package org.collectionspace.chain.util.jpath;

public class InvalidJPathException extends Exception {
	private static final long serialVersionUID = 5102348119739177775L;

	public InvalidJPathException() {
		super();
	}

	public InvalidJPathException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidJPathException(String message) {
		super(message);
	}

	public InvalidJPathException(Throwable cause) {
		super(cause);
	}
}
