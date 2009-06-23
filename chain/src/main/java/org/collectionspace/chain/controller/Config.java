package org.collectionspace.chain.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

public class Config {
	private static final String CHAIN_PROPERTIES="chain.properties";
	private static final String STORE_PROPERTY="cspace.chain.store.dir";
	private static final String SCHEMA_PROPERTY="cspace.chain.schema.dir";

	private ServletContext ctx;
	private Properties props;

	public Config(ServletContext ctx) throws IOException {
		// Load properties file, if present
		InputStream is=Thread.currentThread().getContextClassLoader().getResourceAsStream(CHAIN_PROPERTIES);
		props=new Properties();
		if(is==null) {
			System.err.println("Warning: no configuration found"); // XXX do logging properly
			return;  // Missing is as blank
		}
		props.load(is);
		is.close();
		this.ctx=ctx;
	}

	private synchronized String testStore(String suffix) {
		try {
			String dir=(String)ctx.getAttribute("test-store");
			if(dir==null)
				return null;
			File base=new File(dir,suffix);
			if(!base.exists())
				base.mkdir();
			System.err.println("testing path "+base);
			return base.getCanonicalPath();
		} catch (IOException e) {
			return null;
		}
	}

	/** Get path to the store for data. We first look for the system property cspace.chain.store.dir. If that's 
	 * missing, we look for a properties file called chain.properties on the classpath and look in there. 
	 * Failing that, we just use the defined java temporary directory.
	 * 
	 * @return The path as a string. Should be a file.
	 */
	public String getPathToStore() {
		// Check if defined test store
		String out=testStore("store");
		if(out!=null)
			return out;
		// Check in properties file
		out=props.getProperty(STORE_PROPERTY);
		if(out!=null)
			return out;
		// Check for system property
		out=System.getProperty(STORE_PROPERTY);
		if(out!=null)
			return out;
		// Use temporary directory
		out=System.getProperty("java.io.tmpdir");
		System.err.println("Warning: Defaulting to tmpdir for storage"); // XXX do logging properly
		System.err.println("Debug: Using store path "+out); // XXX do logging properly
		return out;
	}

	public String getPathToSchemaDocs() {
		// Check if defined test store
		String out=testStore("schema");
		if(out!=null)
			return out;
		// Check in properties file
		out=props.getProperty(SCHEMA_PROPERTY);
		if(out!=null)
			return out;
		// Check for system property
		out=System.getProperty(SCHEMA_PROPERTY);
		if(out!=null)
			return out;
		// Use temporary directory
		out=System.getProperty("java.io.tmpdir");
		System.err.println("Warning: Defaulting to tmpdir for schema"); // XXX do logging properly
		System.err.println("Debug: Using schema path "+out); // XXX do logging properly
		return out;
	}
}
