package org.collectionspace.chain.config;

public class ConfigLoadFailedException extends Exception {
	public ConfigLoadFailedException() { super();  }
	public ConfigLoadFailedException(String message) { super(message); }
	public ConfigLoadFailedException(Throwable cause) { super(cause); }
	public ConfigLoadFailedException(String message, Throwable cause) { super(message, cause); }
}
