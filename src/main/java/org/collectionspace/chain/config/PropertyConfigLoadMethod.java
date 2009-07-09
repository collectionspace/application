/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config;

import java.io.InputStream;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

/** method to load from properties file to get parameter */
public class PropertyConfigLoadMethod extends StringReadingConfigLoadMethod {
	private Properties props=new Properties();

	private void loadProperties(String name) throws ConfigLoadFailedException {
		try {
			InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
			if(is!=null) {
				props.load(is);
				is.close();
			}
		} catch(Exception e) {
			throw new ConfigLoadFailedException("Error loading properties file",e);
		}
	}

	public void init(ConfigLoadController controller,Document root) throws ConfigLoadFailedException {
		Node location=root.selectSingleNode("config/properties");
		if(location!=null && (location instanceof Element))
			loadProperties(((Element)location).getTextTrim());
	}

	@Override 
	public String string_get(String value) {
		return props.getProperty(value);
	}
}
