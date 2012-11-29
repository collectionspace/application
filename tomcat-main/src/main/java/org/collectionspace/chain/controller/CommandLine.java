package org.collectionspace.chain.controller;

/* Invoke from command line with recordtype, domain, maketype (core/delta) and configfile
 * 
 * configfile is the config for the tenant in question.
 * record is the type of the record according to its name in the URL of the service (eg "collectionobjects")
 * domain is the section of the XML to generate the XSD for (eg "collectionspace_core")
 * maketype should be core. The other value, delta, is experimental.
 * 
 * eg java -jar cspace/conf/cspace.jar collectionobjects collectionspace_core core lifesci-tenant.xml
 * 
 */

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.installation.XsdGeneration;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppConfigDeployFileFilter implements FilenameFilter {
    @Override
	public boolean accept(File dir, String name) {
        return (name.startsWith("cspace-config-") && name.endsWith(".xml"));
    }
}

class AppConfigBuildFileFilter implements FilenameFilter {
    @Override
	public boolean accept(File dir, String name) {
        return (name.endsWith("-tenant.xml"));
    }
}

public class CommandLine {
	private static final Logger log = LoggerFactory.getLogger(CommandLine.class);
	private static final String TENANT_CONFIG_DIR = "lib";
	private static boolean fromBuild;

	private static final FilenameFilter getConfigFileFilter(File configDir) {
		FilenameFilter result = new AppConfigBuildFileFilter();
		setFromBuild(true);
		
		Map<String, String> env = System.getenv();
		String jeeHomeDir = env.get(ConfigFinder.CSPACE_JEESERVER_HOME);
		if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
			if (configDir.getAbsolutePath().startsWith(jeeHomeDir) == true) {
				setFromBuild(false);
				result = new AppConfigDeployFileFilter();
			}
		}
		
		return result;
	}
	
	private static final File getTenantConfigDir(String[] args) {
		File result = null;
		String errMsg = null;
		
		String configDirName = null;
		if (args != null && args.length > 0 && args[0].trim().isEmpty() == false) {
			configDirName = args[0];
		} else {
			// Look for the JEE server path in the environment vars
			Map<String, String> env = System.getenv();
			String jeeHomeDir = env.get(ConfigFinder.CSPACE_JEESERVER_HOME);
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				configDirName = jeeHomeDir + "/" + TENANT_CONFIG_DIR;
			}
			log.info(String.format("No command line arguments specified.  Using system environment variable '%s'='%s' to locate tenant config files.",
					ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDir));
		}
		
		if (configDirName != null) {
			File dir = new File(configDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				result = dir;
			} else {
				errMsg = String.format("The App config directory '%s' does not exist.", dir.getAbsolutePath());
			}
		}
		
		if (errMsg != null) {
			log.error(errMsg);
		}
		
		return result;
	}
	
	/*
	 * Returns the list of tenant configuration files.
	 */
	private static final File[] getListOfTenantConfigFiles(File configDir) {
		File[] result = {new File("")};
		
		if (configDir.exists() && configDir.isDirectory()) {
			System.out.println(String.format("The App config directory: '%s'", configDir.getAbsolutePath()));
			String[] fileNameList = configDir.list(getConfigFileFilter(configDir));
			if (fileNameList != null && fileNameList.length > 0) {
				ArrayList<File> fileList = new ArrayList<File>();
				for (String fileName : fileNameList) {
					fileList.add(new File(configDir.getAbsolutePath() + "/" + fileName));
					log.debug(String.format("Found tenant config file: '%s'", fileName));
				}
				result = fileList.toArray(result);
			} else {
				System.err.println(String.format("No App tenant config files exist in '%s'", configDir.getAbsolutePath()));
			}
		} else {
			System.err.println(String.format("The App config argument '%s' does not exist or is not a directory.", configDir.getAbsolutePath()));
		}
		
		return result;
	}
	
	public static final void main(String[] args) throws Exception {
		String recordtype = "intakes";//args[0];
		String domain = "collectionspace_core"; //args[1];
		String maketype = "core"; //args[2];
		String configfile = "core-tenant.xml"; //args[3];
		
		File tenantConfigDir = getTenantConfigDir(args);
		if (tenantConfigDir != null && tenantConfigDir.exists() == true) {
			File[] tenantConfigFileList = getListOfTenantConfigFiles(tenantConfigDir);
			String errMsg = null;
			for (File tenantConfigFile : tenantConfigFileList) {
				//
				// Generate all the Service schemas from the Application layer's configuration records
				//
				try {
					XsdGeneration s = new XsdGeneration(tenantConfigFile, maketype, "3.0");
					HashMap<String, String> xsdschemas = s.getServiceSchemas();
					
					System.out.println(String.format("Record type: %s", recordtype));
					for (String schemaName : xsdschemas.keySet()) {
						System.out.println(String.format("\tSchema file name: %s", schemaName));
						//System.out.println(xsdschemas.get(schemaName));
					}
				} catch (Exception e) {
					String exceptionMsg = e.getMessage();
					if (errMsg != null) {
						errMsg = errMsg + "\r\n" + exceptionMsg;
					} else {
						errMsg = exceptionMsg;
					}
				}
			}
			
			if (errMsg != null) {
				System.err.println(errMsg);
			}
		} else {
			String errMsg = String.format("Either a command line argument specifying the Application layer's tenant config directory needs to be supplied or a" +
					" system environment variable '%s' needs to be set and pointing to the CollectionSpace JEE server installation directory.",
					ConfigFinder.CSPACE_JEESERVER_HOME);
			if (tenantConfigDir != null && tenantConfigDir.exists() == false) {
				errMsg = String.format("The command line argument '%s' specifying the Application layer's tenant config directory is incorrect.",
						tenantConfigDir != null ? tenantConfigDir.getAbsolutePath() : "<unspecified>");
			}
			log.error(errMsg);
			System.exit(-1);
			return; // exits with error message to log
		}
	}


	public static boolean isFromBuild() {
		return fromBuild;
	}


	public static void setFromBuild(boolean fromBuild) {
		CommandLine.fromBuild = fromBuild;
	}
}
