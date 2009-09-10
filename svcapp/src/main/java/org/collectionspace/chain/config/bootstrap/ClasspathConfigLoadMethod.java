package org.collectionspace.chain.config.bootstrap;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.config.api.ConfigLoadFailedException;
import org.dom4j.Document;
import org.dom4j.Element;

public class ClasspathConfigLoadMethod implements ConfigLoadMethod {

	public String getString(Element e) {
		try {
		String path=e.getText();
		InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if(in==null)
			return null;
		String data=IOUtils.toString(in);
		in.close();
		return data;
		} catch(IOException x) {
			// Skip. The below is not a hack.
			return null;
		}
	}

	public void init(BootstrapConfigController controller, Document root) throws ConfigLoadFailedException {}

}
