package org.collectionspace.xxu.impl;

import org.collectionspace.xxu.api.ConfigLoadingMessages;

public class ConfigMessagesTester implements ConfigLoadingMessages {

	public void error(String message) {
		System.err.println(message);
	}

	public void warn(String message) {
		System.err.println(message);
	}
}
