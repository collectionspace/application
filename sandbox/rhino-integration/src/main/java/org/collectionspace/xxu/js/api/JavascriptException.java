package org.collectionspace.xxu.js.api;

public class JavascriptException extends Exception {
	private static final long serialVersionUID = 2025245098493025128L;
	public JavascriptException() { super(); }
	public JavascriptException(String s) { super(s); }
	public JavascriptException(Exception x) { super(x); }
	public JavascriptException(String s,Exception x) { super(s,x); }
}
