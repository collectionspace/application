package org.collectionspace.xxu.test.main;

public class InvalidConfigException extends Exception {
	public InvalidConfigException() {}
	public InvalidConfigException(String s) { super(s); }
	public InvalidConfigException(Exception x) { super(x); }
	public InvalidConfigException(String s,Exception x) { super(s,x); }
}
