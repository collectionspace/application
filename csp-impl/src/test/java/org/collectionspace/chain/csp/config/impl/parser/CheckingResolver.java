package org.collectionspace.chain.csp.config.impl.parser;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class CheckingResolver implements EntityResolver {
	@Override public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+systemId;
		InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		return new InputSource(in);
	}			
}