package org.collectionspace.chain.util.jtmpl;

public class InvalidJTmplException extends Exception {
	private static final long serialVersionUID = 4708546839564877741L;

	public InvalidJTmplException() { super(); }
	public InvalidJTmplException(String message) { super(message); }
	public InvalidJTmplException(Throwable cause) { super(cause); }
	public InvalidJTmplException(String message, Throwable cause) { super(message, cause); }
}
