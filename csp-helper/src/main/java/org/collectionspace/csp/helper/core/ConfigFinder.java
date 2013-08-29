/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.csp.helper.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.collectionspace.csp.api.core.CSPDependencyException;

/** Here's where we look for the config. 
 * 
 * First we look for an environment variable containing the actual config.
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
public class ConfigFinder implements EntityResolver {
	private static final Logger log=LoggerFactory.getLogger(ConfigFinder.class);
	private static final String classNearDefaultXml = "org.collectionspace.chain.controller.ChainServlet";
	private static final String TEST_CONFIG = "TEST_CONFIG"; 
	public static final String CSPACE_JEESERVER_HOME = "CSPACE_JEESERVER_HOME";

	private ServletContext ctx;
	private File configBase;

	public ConfigFinder(ServletContext ctx) {
		this(ctx, null);
	}

	public ConfigFinder(ServletContext ctx, File configBase) {
		this.ctx=ctx;
		if (configBase != null) {
			this.setConfigBase(configBase);
			if (log.isDebugEnabled() == true) {
				log.debug(String.format("App configuration base directory is: '%s'", configBase.getAbsolutePath()));
			}
		}
	}

	private InputStream getDataFromAttribute() {
		String out=(String)ctx.getAttribute("config-data");
		if(out==null)
			return null;
		return IOUtils.toInputStream(out);
	}

	private File getDataFromAttributePathAsFile() throws IOException {
		File result = null;
		
		String out = (String)ctx.getAttribute("config-path");
		if (out != null) {
			File file = new File(out);
			if (file.exists() == true) {
				result = file;
			}
		}
		
		return result;
	}

	private InputStream getDataFromAttributePath() throws IOException {
		InputStream result = null;
		
		File file = getDataFromAttributePathAsFile();
		if (file != null) {
			result = new FileInputStream(file);
		}
		
		return result;
	}

	private InputStream getDataFromName() {
		InputStream result = null;
		String path = (String)ctx.getAttribute("config-filename");

		try {
			result = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		} catch (Exception x) {
			log.trace(String.format("Could not find App config file from context attribute name '%s'", "config-filename"),
					x);
		}
		
		return result;
	}

	private File getDataFromNameAsFile() {
		File result = null;

		String path = (String) ctx.getAttribute("config-filename");
		try {
			URL fileURL = Thread.currentThread().getContextClassLoader()
					.getResource(path);
			String fileName = fileURL.getFile();
			if (fileName != null) {
				File file = new File(fileName);
				if (file.exists() == true) {
					result = file;
				} else {
					log.error(String.format("File with name '%s' from URL '%s' does not exist.", fileName, fileURL.toString()));
				}
			} else {
				log.error(String.format("File from URL '%s' does not exist.", fileURL.toString()));
			}
		} catch (Exception x) {
			log.error(x.toString());
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private static InputStream getDataFromJBossPath(String filename) {
		try {
			File file=new File(System.getProperty("jboss.home.dir")+"/server/cspace/conf/"+filename);
			Properties p=System.getProperties();
			for(Entry k : p.entrySet()) {
				log.trace(" property "+k.getKey()+" = "+k.getValue());
			}
			log.debug("A Looking in "+System.getProperty("jboss.home.dir")+"/server/cspace/conf/"+filename);
			if(!file.exists())
				return null;
			return new FileInputStream(file);			
		} catch(Exception x) { return null; }
	}

	@SuppressWarnings("unchecked")
	private static File getDataFromJBossPathAsFile(String filename) {
		File result = null;

		try {
			File file = new File(System.getProperty("jboss.home.dir")
					+ "/server/cspace/conf/" + filename);
			Properties p = System.getProperties();
			for (Entry k : p.entrySet()) {
				log.trace(" property " + k.getKey() + " = " + k.getValue());
			}
			log.debug("A Looking in " + System.getProperty("jboss.home.dir")
					+ "/server/cspace/conf/" + filename);
			if (file.exists() == true) {
				result = file;
			} else {
				log.error(String.format("App config file '%s' could not be found.", filename));
			}
		} catch (Exception x) {
			log.error(String.format("App config file '%s' could not be found.", filename));
		}

		return result;
	}

	private static InputStream getDataFromEnvironmentVariable() {
		InputStream result = null;
		
		File file = getDataFromEnvironmentVariableAsFile();
		if (file != null) {
			try {
				result = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		return result;
	}

	private static File getDataFromEnvironmentVariableAsFile() {
		File result = null;

		try {
			String filename = System.getenv(TEST_CONFIG);
			if (filename != null) {
				File file = new File(filename);
				if (file.exists() == true) {
					result = file;
				}
			}
		} catch (Exception x) {
			log.trace(String.format("Encountered an exception trying to find environment variable '%s'.", TEST_CONFIG));
		}

		return result;
	}

	private static InputStream getDataFromClasspath(String filename) {
		try {
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		} catch(Exception x) { return null; }
	}

	private static File getDataFromClasspathAsFile(String configFileName) {
		File result = null;
		
		try {
			URL fileURL = Thread.currentThread().getContextClassLoader()
					.getResource(configFileName);
			if (fileURL != null) {
				String fileName = fileURL.getFile();
				if (fileName != null) {
					File file = new File(fileName);
					if (file.exists() == true) {
						result = file;
					} else {
						log.error(String.format("App config file '%s' from URL '%s' could not be found.", fileName, fileURL));
					}
				} else {
					log.error(String.format("App config file '%s' could not be found.", fileName, fileURL));
				}
			}
		} catch (Exception x) {
			log.error(String.format("App config file '%s' could not be found.", configFileName), x);
		}
		
		return result;
	}

	private static InputStream getConfigStreamViaClassLoader(String configFilename) throws CSPDependencyException {
		// TODO next stage will be to move default.xml into here and rename it (CSPACE-1288)
		// CSPACE-2114 initial (still messy, but better than before) stage is to change 
		// from 3 files (2x config and default) to just one of them (default.xml hard-coded here)

		ClassLoader loader;
		try {
			// In Eclipse all classes are still visible so we can jump across via a known class name
			loader = Class.forName(classNearDefaultXml).getClassLoader();
		} catch (ClassNotFoundException e) {
			// In Maven we can only see stuff in target/test-classes
			// so this relies on top-level pom.xml copying the file in for us 
			log.debug("Falling back to current thread ClassLoader");
			loader = Thread.currentThread().getContextClassLoader();
		}
		try {
			InputStream result = loader.getResourceAsStream(configFilename);
			if(result!=null) {
				log.debug("Found config for testing: "+configFilename);
				URL configFileURL = loader.getResource(configFilename);
				log.debug(String.format("Config file URL: '%s'", configFileURL.toString()));
				log.debug(String.format("URL file: '%s'", configFileURL.getFile()));
				return result;
			} else {
				throw new CSPDependencyException("Failed to open config file at "+configFilename);
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	private static File getConfigFileViaClassLoader(String configFilename) throws CSPDependencyException {
		File result = null;
		ClassLoader loader;
		try {
			// In Eclipse all classes are still visible so we can jump across via a known class name
			loader = Class.forName(classNearDefaultXml).getClassLoader();
		} catch (ClassNotFoundException e) {
			// In Maven we can only see stuff in target/test-classes
			// so this relies on top-level pom.xml copying the file in for us 
			log.debug("Falling back to current thread ClassLoader");
			loader = Thread.currentThread().getContextClassLoader();
		}
		try {
			URL fileURL = loader.getResource(configFilename);
			if (fileURL != null) {
				log.debug("Found config for testing: " + configFilename);
				log.debug(String.format("Config file URL: '%s'", fileURL.toString()));
				String filename = fileURL.getFile();
				log.debug(String.format("URL file: '%s'", filename));
				File file = new File(filename);
				if (file.exists() == true) {
					result = file;
				} else {
					log.error(String.format("App config file '%s' from URL '%s' does not exist.", filename, fileURL));
				}
			} else {
				throw new CSPDependencyException("Failed to open config file at " + configFilename);
			}
		} catch (Exception e) {
			log.error(String.format("Could not load config file '%s'", configFilename), e);
		}
		
		return result;
	}	

	/*
	 * Try to resolve/find and load a resource/entity/file.  Uses a variety of method and locations to resolve the resource/entity/file
	 * (non-Javadoc)
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		try {
			InputStream out = null;
			
			// If there is a conf base directory, look for it there first			
			File configBase = this.getConfigBase();
			if (configBase != null && configBase.exists()) {
				String fileEntityName = configBase.getAbsolutePath() + "/" + systemId;
				log.debug(String.format("Looking to resolve publicId:%s systemId:%s in '%s'.", publicId, systemId,
						fileEntityName));
				File fileEntity = new File(fileEntityName);
				if (fileEntity.exists() == true) {
					log.debug(String.format("Resolved '%s'.\r\n", fileEntityName));
					out = new FileInputStream(fileEntity);
					return new InputSource(out);
				}
			}
			
			// Look for it in the env vars
			out = getDataFromEnvironmentVariable();
			if (out != null) {
				return new InputSource(out);
			}

			// Look for it as a servlet container attribute
			if (ctx != null && "-//CSPACE//ROOT".equals(publicId)) {
				out = getDataFromAttribute();
				if (out != null) {
					return new InputSource(out);
				}

				out = getDataFromAttributePath();
				if (out != null) {
					return new InputSource(out);
				}

				out = getDataFromName();
				if (out != null) {
					return new InputSource(out);
				}

			}

			// use config from tomcat-main/src/main/resources if this is a test
			// run by mvn
			if ("-//CSPACE//TESTROOT".equals(publicId)) {
				out = getConfigStreamViaClassLoader(systemId);
				if (out != null)
					return new InputSource(out);
			}
			out = getDataFromJBossPath(systemId);
			if (out != null)
				return new InputSource(out);
			out = getDataFromClasspath(systemId); // running tests find the
													// resource/entity here
			if (out != null)
				return new InputSource(out);
			out = getConfigStreamViaClassLoader(systemId);
			if (out != null)
				return new InputSource(out);
			throw new SAXException("No such file " + systemId);
		} catch (CSPDependencyException e) {
			throw new SAXException("Error parsing", e);
		}
	}

	public File getConfigBase() {
		return configBase;
	}

	public void setConfigBase(File configBase) {
		this.configBase = configBase;
	}

	public File resolveEntityAsFile(String publicId, String systemId)
			throws SAXException, IOException {
		try {
			File out = getDataFromEnvironmentVariableAsFile();
			if (out != null) {
				return out;
			}

			if (ctx != null && "-//CSPACE//ROOT".equals(publicId)) {
				InputStream outStream = getDataFromAttribute();
				if (outStream != null) {
					log.warn(String.format("Configuration not found as a file but as a string in the environment: %s",
							outStream.toString()));
					return null;
				}

				out = getDataFromAttributePathAsFile();
				if (out != null) {
					return out;
				}

				out = getDataFromNameAsFile();
				if (out != null) {
					return out;
				}
			}

			// use config from tomcat-main/src/main/resources if this is a test
			// run by mvn
			if ("-//CSPACE//TESTROOT".equals(publicId)) {
				out = getConfigFileViaClassLoader(systemId);
				if (out != null) {
					return out;
				}
			}

			out = getDataFromJBossPathAsFile(systemId);
			if (out != null) {
				return out;
			}
			
			out = getDataFromClasspathAsFile(systemId); // running tests find the resource/entity here
			if (out != null) {
				return out;
			}
			
			out = getConfigFileViaClassLoader(systemId);
			if (out != null) {
				return out;
			}
			
			throw new SAXException("No such file " + systemId);
		} catch (CSPDependencyException e) {
			throw new SAXException("Error parsing", e);
		}
	}
}
