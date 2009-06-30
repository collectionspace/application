package org.collectionspace.chain.util.xtmpl;

public class InvalidXTmplException extends Exception {
	private static final long serialVersionUID = -9033938559839598685L;

	public InvalidXTmplException() { super(); }

	public InvalidXTmplException(String message) { super(message); }

	public InvalidXTmplException(Throwable cause) { super(cause); }

	public InvalidXTmplException(String message, Throwable cause) { super(message, cause); }
}
