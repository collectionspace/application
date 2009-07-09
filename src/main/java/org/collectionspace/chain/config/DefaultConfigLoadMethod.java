
package org.collectionspace.chain.config;

import org.dom4j.Document;
import org.dom4j.Element;

public class DefaultConfigLoadMethod extends StringReadingConfigLoadMethod implements ConfigLoadMethod {
	@Override
	protected String string_get(String value) { return value; }
}
