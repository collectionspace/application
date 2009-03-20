package org.collectionspace.xxu.api;

public class ConfigLoadingException extends Exception {
	public ConfigLoadingException() { super(); }
	public ConfigLoadingException(String m) { super(m); }
	public ConfigLoadingException(Exception x) { super(x); }
	public ConfigLoadingException(String m,Exception x) { super(m,x); }
}
