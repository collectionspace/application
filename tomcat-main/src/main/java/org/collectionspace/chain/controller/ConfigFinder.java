package org.collectionspace.chain.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;

/** Here's where we look for the config. 
 * 
 * First we look for a servlet data containing the actual config.
 * Next we look for a servlet attribute specifying a path.
 * Next we look in ${jboss.home.dir}/server/cspace/conf/cspace-config.xml
 * Next we look in the classpath for a name supplied in a servlet attribute.
 * Next we look in the classpath for something called cspace-config.xml
 * (Tomcat 5  = $TOMCAT_HOME/shared/classes
 * Tomcat 6 = $TOMCAT_HOME/lib)
 * Finally we fail.
 * 
 * @author dan
 *
 */
public class ConfigFinder {
	private static final Logger log=LoggerFactory.getLogger(ConfigFinder.class);
	private static InputStream getDataFromAttribute(ServletContext ctx) {
		String out=(String)ctx.getAttribute("config-data");
		if(out==null)
			return null;
		return IOUtils.toInputStream(out);
	}
	
	private static InputStream getDataFromAttributePath(ServletContext ctx) throws IOException {
		String out=(String)ctx.getAttribute("config-path");
		//log.info("B Looking in "+out);
		if(out==null)
			return null;
		File file=new File(out);
		if(!file.exists())
			return null;
		return new FileInputStream(file);
	}
	
	private static InputStream getDataFromName(ServletContext ctx) {
		String filename=(String)ctx.getAttribute("config-filename");
		//log.info("C Looking in "+filename);	
		try {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		} catch(Exception x) { return null; }
	}

	private static InputStream getDataFromJBossPath() {
		try {
			File file=new File(System.getProperty("jboss.home.dir")+"/server/cspace/conf/cspace-config.xml");
			Properties p=System.getProperties();
			for(Entry k : p.entrySet()) {
				log.info(" property "+k.getKey()+" = "+k.getValue());
			}
			log.info("A Looking in "+System.getProperty("jboss.home.dir")+"/server/cspace/conf/cspace-config.xml");
			if(!file.exists())
				return null;
			return new FileInputStream(file);			
		} catch(Exception x) { return null; }
	}

	private static InputStream getDataFromClasspath() {
		try {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream("cspace-config.xml");
		} catch(Exception x) { return null; }
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
		out=getDataFromJBossPath();
		if(out!=null)
			return out;
		return getDataFromClasspath();
	}
}
