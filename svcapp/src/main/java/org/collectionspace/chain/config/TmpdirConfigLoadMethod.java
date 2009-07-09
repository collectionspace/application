package org.collectionspace.chain.config;

import org.collectionspace.chain.controller.Config;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TmpdirConfigLoadMethod implements ConfigLoadMethod {
	private static final Logger log=LoggerFactory.getLogger(TmpdirConfigLoadMethod.class);
	
	public String getString(Element e) {
		String out=System.getProperty("java.io.tmpdir");
		log.warn("Warning: Defaulting to tmpdir for "+e.getName());
		return out;
	}

	public void init(ConfigLoadController controller, Document root)
			throws ConfigLoadFailedException {}
}
