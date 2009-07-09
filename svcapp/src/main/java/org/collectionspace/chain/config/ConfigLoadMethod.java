package org.collectionspace.chain.config;

import org.dom4j.Document;
import org.dom4j.Element;

public interface ConfigLoadMethod {	
	public void init(ConfigLoadController controller,Document root) throws ConfigLoadFailedException;
		
	public String getString(Element e);
}
