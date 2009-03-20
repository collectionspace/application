package org.collectionspace.xxu.csp.transform;

import java.io.IOException;
import java.io.InputStream;

import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.CSPProviderFactory;
import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.dom4j.Node;

public class ProviderTransformFactory implements CSPProviderFactory {

	public CSPProvider process(CSP csp,Node in) throws ConfigLoadingException {
		if(!"transform".equals(in.getName()))
			return null;
		String file=in.valueOf("@file");
		if(file==null || "".equals(file))
			throw new ConfigLoadingException("Must supply file attribute to transform");
		InputStream xform=csp.getFileStream(file);
		if(xform==null)
			throw new ConfigLoadingException("Invalid file attribute to transform");
		CSPProvider out=new CSPProviderTransformImpl(xform);
		try {
			xform.close();
		} catch (IOException e) {
			throw new ConfigLoadingException("Unexpected error closing transform file",e);
		}
		return out;
	}
}
