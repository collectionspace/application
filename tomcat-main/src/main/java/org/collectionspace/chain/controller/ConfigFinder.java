package org.collectionspace.chain.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;

/** Here's where we look for the config. 
 * 
 * First we look for a servlet data containing the actual config.
 * Next we look for a servlet attribute specifying a path.
 * Next we look in the classpath for a name supplied in a servlet attribute.
 * Next we look in the classpath for something called cspace-config.xml
 * Finally we fail.
 * 
 * @author dan
 *
 */
public class ConfigFinder {
	private static InputStream getDataFromAttribute(ServletContext ctx) {
		String out=(String)ctx.getAttribute("config-data");
		if(out==null)
			return null;
		return IOUtils.toInputStream(out);
	}
	
	private static InputStream getDataFromAttributePath(ServletContext ctx) throws IOException {
		String out=(String)ctx.getAttribute("config-path");
		if(out==null)
			return null;
		File file=new File(out);
		if(!file.exists())
			return null;
		return new FileInputStream(file);
	}
	
	private static InputStream getDataFromName(ServletContext ctx) {
		String filename=(String)ctx.getAttribute("config-filename");		
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
	}

	private static InputStream getDataFromClasspath() {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream("cspace-config.xml");
	}
	
	public static InputStream getConfig(ServletContext ctx) throws IOException {
		InputStream out=getDataFromAttribute(ctx);
		if(out!=null)
			return out;
		out=getDataFromAttributePath(ctx);
		if(out!=null)
			return out;
		out=getDataFromName(ctx);
		if(out!=null)
			return out;
		return getDataFromClasspath();
	}
}
