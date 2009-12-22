/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;

/** method to load from properties file to get parameter */
public class PropertyConfigLoadMethod implements ConfigLoadMethod {
	private static Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
	
	private Map<String,Properties> prop_files=new HashMap<String,Properties>();

	private String substitute_system_props(String in) {
		Matcher m = p.matcher(in);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String value=System.getProperty(m.group(1));
			if(value==null)
				value="";
			System.err.println(m.group(1));
			m.appendReplacement(sb,value);
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private Properties loadProperties(String name) throws BootstrapConfigLoadFailedException {
		try {
			Properties out=new Properties();
			String path=substitute_system_props(name);
			InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
			if(is!=null) {
				System.err.println("Using bootstrap source in classpath at "+path);
			}
			if(is==null) {
				// Try filesystem
				is=new FileInputStream(new File(path));
				if(is!=null) {
					System.err.println("Using bootstrap source in filesystem at "+path);					
				}
			}
			if(is!=null) {
				out.load(is);
				is.close();
			} else {
				System.err.println("Cannot find bootstrap source for "+name);
			}
			return out;
		} catch(Exception e) {
			// Fall through. Okay, we just won't use this one.
			System.err.println("Cannot find bootstrap source for "+name);
			return null;
		}
	}

	public void init(BootstrapConfigController controller,Document root) throws BootstrapConfigLoadFailedException {
		for(Object location : root.selectNodes("config/properties")) {
			if(!(location instanceof Element))
				continue;
			String name=((Element)location).attributeValue("name");
			if(StringUtils.isBlank(name))
				name="";
			Properties p=loadProperties(((Element)location).getTextTrim());
			if(p!=null)
				prop_files.put(name,p);
		}
	}

	public String getString(Element e) {
		String src=e.attributeValue("src");
		if(StringUtils.isBlank(src))
			src="";
		Properties props=prop_files.get(src);
		if(props==null)
			return null;
		return props.getProperty(e.getTextTrim());
	}
}
