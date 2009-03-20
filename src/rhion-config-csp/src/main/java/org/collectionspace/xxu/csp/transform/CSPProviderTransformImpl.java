package org.collectionspace.xxu.csp.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.ConfigLoadingException;

public class CSPProviderTransformImpl implements CSPProvider {
	private byte[] xform;
	
	public CSPProviderTransformImpl(InputStream in) throws ConfigLoadingException {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			IOUtils.copy(in,baos);
		} catch (IOException e) {
			throw new ConfigLoadingException("Could not copy XSLT",e);
		}
		xform=baos.toByteArray();
	}
	
	public void act(ConfigLoader in) throws ConfigLoadingException {
		in.addXSLT(new ByteArrayInputStream(xform));
	}
}
