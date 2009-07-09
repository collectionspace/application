package org.collectionspace.chain.config;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeConfigLoadMethod implements ConfigLoadMethod {
	private ServletContext ctx;
	private static final Logger log=LoggerFactory.getLogger(AttributeConfigLoadMethod.class);
	
	public void init(ConfigLoadController controller,Document root) {
		ctx=controller.getServletContext();
	}
	
	private String mkdir(String in,String suffix) {
		File base=new File(in,suffix);
		if(!base.exists())
			base.mkdir();
		log.info("Using testing path "+base);
		try {
			return base.getCanonicalPath();
		} catch (IOException e) {
			return null;
		}
	}
	
	public String getString(Element e) {
		String out=(String)ctx.getAttribute(e.getTextTrim());
		String mkdir=e.attributeValue("mkdir");
		if(!StringUtils.isBlank(mkdir))
			out=mkdir(out,mkdir);
		return out;
	}
}
