/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.io.IOUtils;

public class StreamDataSource implements DataSource {
	private String mime_type;
	private byte[] data;
	
	public StreamDataSource(InputStream source,String mime_type) throws IOException {
		this.mime_type=mime_type;
		data=IOUtils.toByteArray(source);
	}
	
	public String getContentType() { return mime_type; }

	public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(data); }

	public String getName() { return "[a stream]"; }

	public OutputStream getOutputStream() throws IOException {
		throw new IOException(getClass().getCanonicalName()+" is readonly");
	}

}
