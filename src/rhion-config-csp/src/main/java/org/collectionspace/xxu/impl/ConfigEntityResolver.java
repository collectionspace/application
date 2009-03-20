package org.collectionspace.xxu.impl;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ConfigEntityResolver implements EntityResolver {
	public InputSource resolveEntity(String public_id, String system_id)
			throws SAXException, IOException {
		System.err.println("system="+system_id+" public="+public_id);
		return null;
	}
}
