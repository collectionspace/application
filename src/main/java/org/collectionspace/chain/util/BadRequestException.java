package org.collectionspace.chain.util;

public class BadRequestException extends Exception {
	private static final long serialVersionUID = -129549065899185645L;

	public BadRequestException() {
	}

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(Throwable cause) {
		super(cause);
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
