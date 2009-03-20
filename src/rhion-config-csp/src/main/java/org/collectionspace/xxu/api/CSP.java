package org.collectionspace.xxu.api;

import java.io.File;
import java.io.InputStream;

public interface CSP {
	public CSPMetadata getMetadata();
	public InputStream getFileStream(String name) throws ConfigLoadingException;
}
