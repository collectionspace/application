package org.collectionspace.chain.config;

import org.dom4j.Document;
import org.dom4j.Element;

public abstract class StringReadingConfigLoadMethod implements ConfigLoadMethod {
	protected String value;
	
	protected abstract String string_get(String value);

	public void init(ConfigLoadController controller,Document root) throws ConfigLoadFailedException {}
	
	public final String getString(Element e) {
		return string_get(e.getTextTrim());
	}
}
