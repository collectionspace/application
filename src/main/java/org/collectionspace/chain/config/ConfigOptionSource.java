package org.collectionspace.chain.config;

import org.dom4j.Element;

public class ConfigOptionSource {
	private Element element;
	private ConfigLoadMethod method;
	private String value;
	
	public ConfigOptionSource(ConfigLoadMethod method,Element element) {
		this.method=method;
		this.element=element;
	}
	
	public String getString() {
		if(value==null) {
			synchronized(this) {
				if(value==null)
					value=method.getString(element);
			}
		}
		return value;
	}
}
