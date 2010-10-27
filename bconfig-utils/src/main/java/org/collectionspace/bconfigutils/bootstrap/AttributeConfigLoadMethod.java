/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Method to use servlet attribute (useful during testing)
 */
public class AttributeConfigLoadMethod implements ConfigLoadMethod {
	private ServletContext ctx;
	private static final Logger log=LoggerFactory.getLogger(AttributeConfigLoadMethod.class);
	
	public void init(BootstrapConfigController controller,Document root) {
		ctx=controller.getServletContext();
	}
	
	private String mkdir(String in,String suffix) {
		File base=new File(in,suffix);
		if(!base.exists())
			base.mkdir();
		log.debug("Using testing path "+base);
		try {
			return base.getCanonicalPath();
		} catch (IOException e) {
			return null;
		}
	}
	
	public String getString(Element e) {
		if(ctx==null)
			return null;
		String out=(String)ctx.getAttribute(e.getTextTrim());
		String mkdir=e.attributeValue("mkdir");
		if(out!=null && !StringUtils.isBlank(mkdir))
			out=mkdir(out,mkdir);
		return out;
	}
}
