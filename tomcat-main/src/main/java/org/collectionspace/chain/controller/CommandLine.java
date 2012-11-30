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
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.collectionspace.chain.installation.XsdGeneration;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.services.common.api.JEEServerDeployment;
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
	
	private static final String SCHEMA_AND_DOCTYPES_DIR = JEEServerDeployment.NUXEO_SERVER_DIR;
	private static final String TENANT_CONFIG_DIR = "lib";
	
	private static Options options;
	private static org.apache.commons.cli.CommandLine cmd;
	
	private static final boolean kVALUE_NEEDED = true;
	private static final boolean kNO_VALUE_NEEDED = false;
	
	private static final String ARG_CONFIG_BASE_DIR = "c";
	private static final String ARG_OUTPUT_DIR = "o";
	private static final String ARG_HELP = "h";

	private static boolean fromBuild;
	private static File cspaceHomeDir;
	private static File appConfigDir;
	private static File bundlesOutputDir;
	
	
	private static File getCspaceHomeDir() {
		return cspaceHomeDir;
	}

	private static void setCspaceHomeDir(File dir) {
		cspaceHomeDir = dir;
	}


	private static File getAppConfigDir() {
		return appConfigDir;
	}


	private static void setAppConfigDir(File dir) {
		appConfigDir = dir;
	}


	private static File getBundlesOutputDir() {
		return bundlesOutputDir;
	}


	private static void setBundlesOutputDir(File dir) {
		bundlesOutputDir = dir;
	}

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
	
	private static final File resolveTenantConfigDir() {
		File result = null;
		String errMsg = null;
		
		String configDirName = cmd.getOptionValue(ARG_CONFIG_BASE_DIR);
		if (configDirName == null || configDirName.isEmpty() == true) {
			// If they didn't specify the config dir as a command line argument
			// then look for the JEE server path in the environment vars and derive the config dir from it
			Map<String, String> env = System.getenv();
			String jeeHomeDir = env.get(ConfigFinder.CSPACE_JEESERVER_HOME);
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				configDirName = jeeHomeDir + "/" + TENANT_CONFIG_DIR;
			}
			log.info(String.format("No command line argument for the App config directory was specified so we're using the system environment variable '%s'='%s' to locate the config files.",
					ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDir));
		}
		
		if (configDirName != null) {
			File dir = new File(configDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				setAppConfigDir(dir);
				result = dir;
			} else {
				errMsg = String.format("The App config directory '%s' does not exist.", dir.getAbsolutePath());
			}
		} else {
			errMsg = String.format("No command line argument for the App config directory was specified and the system environment variable '%s' is missing.",
					ConfigFinder.CSPACE_JEESERVER_HOME);
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
		
	private static void showHelp() {
    	// automatically generate the help statement
    	HelpFormatter formatter = new HelpFormatter();
    	formatter.printHelp("options", getOptions());
	}

	private static void showHelp(Exception e) {
		String errMsg = e.getMessage();
		if (errMsg != null) {
			System.err.println(errMsg);
		}
		showHelp();
	}
	
	private static void setCmd(org.apache.commons.cli.CommandLine cmd) {
		CommandLine.cmd = cmd;
	}
	
	private static org.apache.commons.cli.CommandLine getCmd() {
		return CommandLine.cmd;
	}
	
	private static Options getOptions() {
		if (options == null) {
			setOptions();
		}
		
		return options;
	}
	
	private static void setOptions() {
    	// create Options object
    	Options options = new Options();
    	
    	Option newOption;
    	// add option for specifying the Application layer's config base/root directory
    	newOption = new Option(ARG_CONFIG_BASE_DIR, kVALUE_NEEDED, "App base config directory");
    	options.addOption(newOption);

    	// add option for specifying the output directory for the bundles
    	newOption = new Option(ARG_OUTPUT_DIR, kVALUE_NEEDED, "Output directory - current directory is the default");
    	options.addOption(newOption);
    	
    	// add option for help
    	newOption = new Option(ARG_HELP, kNO_VALUE_NEEDED, "Shows help");
    	options.addOption(newOption);
    	
    	CommandLine.options = options;
    	
    	return;
	}
	
	/*
	 * Uses the org.apache.commons.cli package for parsing and handling command line arguments.
	 * See http://commons.apache.org/cli/index.html
	 */
	private static boolean parseArgs(String[] args) {
		boolean result = false;
    	setOptions();
    	CommandLineParser parser = new PosixParser();
    	if (args.length > 0) {
			try {
				cmd = parser.parse(options, args);
				
				@SuppressWarnings("unchecked")
				List<String> unparsedArgs = (List<String>)cmd.getArgList();
				if (unparsedArgs.isEmpty() == false) {
					throw new Exception("Found unexpected command line arugments.");
				}
				
		    	if (cmd.hasOption(ARG_HELP)) {
		    	    showHelp();
		    	    return false;
		    	}
						    	
	    	    result = true;
			} catch (Exception e) {
				showHelp(e);
			}
    	} else {
    		showHelp(new Exception("No arguments specified."));
    	}
    	
    	return result;
    }
	
	public static final void main(String[] args) throws Exception {
		//
		// Parse the commandline arguments.
		//
		boolean parseSuccessful = parseArgs(args);
		if (parseSuccessful == false) {
			System.exit(-1);
		}
		//
		// Make sure we have a place to output the schema and doctype bundles
		//
		File bundleOutputDir = resolveBundleOutputDir();
		//
		// Find the App config base directory and start processing.
		//
		File tenantConfigDir = resolveTenantConfigDir();
		if (tenantConfigDir != null && tenantConfigDir.exists() == true) {
			String maketype = "core"; //args[2];		
			File[] tenantConfigFileList = getListOfTenantConfigFiles(tenantConfigDir);
			String errMsg = null;
			for (File tenantConfigFile : tenantConfigFileList) {
				//
				// Generate all the Service schemas from the Application layer's configuration records
				//
				XsdGeneration xsdMetadata = null;
				try {
					xsdMetadata = new XsdGeneration(tenantConfigFile, maketype, "3.0", bundleOutputDir);
					
					System.out.println(String.format("Schema types defined in config: %s", tenantConfigFile.getAbsolutePath()));
					HashMap<String, String> xsdschemas = xsdMetadata.getServiceSchemas();					
					for (String schemaName : xsdschemas.keySet()) {
						System.out.println(String.format("\tSchema file name: %s", schemaName));
					}
					
					HashMap<String, String> doctypes = xsdMetadata.getServiceDoctypeBundles();
					System.out.println(String.format("Document types defined in config: %s", tenantConfigFile.getAbsolutePath()));
					for (String doctypeName : doctypes.keySet()) {
						System.out.println(String.format("\tDocument type file name: %s", doctypeName));
					}
				} catch (Exception e) {
					String exceptionMsg = e.getMessage();
					if (errMsg != null) {
						errMsg = errMsg + "\r\n" + exceptionMsg; // Append the error message (if any) from each attempt
					} else {
						errMsg = exceptionMsg;
					}
				}
			}
			//
			// Check to see if we encountered any errors.  If so, exist with a negative status.
			//
			if (errMsg != null) {
				System.err.println(errMsg);
				System.exit(-1);
			}
		} else {
			String errMsg = String.format("Could not locate the Application layer's base config directory. See the log file and previous error messages for details.");
			if (tenantConfigDir != null && tenantConfigDir.exists() == false) {
				errMsg = String.format("The command line argument '%s' specifying the Application layer's tenant config directory is incorrect.",
						tenantConfigDir != null ? tenantConfigDir.getAbsolutePath() : "<unspecified>");
			}
			log.error(errMsg);
			System.exit(-1);
		}
	}


	private static File resolveBundleOutputDir() {
		File result = null;
		String errMsg = null;
		
		String outputDirName = cmd.getOptionValue(ARG_OUTPUT_DIR);
		if (outputDirName == null || outputDirName.isEmpty() == true) {
			// If they didn't specify the output dir as a command line argument
			// then look for the JEE server path in the environment vars and derive the output dir from it
			Map<String, String> env = System.getenv();
			String jeeHomeDir = env.get(ConfigFinder.CSPACE_JEESERVER_HOME);
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				outputDirName = jeeHomeDir + "/" + JEEServerDeployment.NUXEO_SERVER_PLUGINS_DIR;
				log.info(String.format("No command line directory argument for the output of the schema and doctype bundles was specified so we're using "
						+ "the system environment variable '%s'='%s' to locate the output directory.",
						ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDir));
			} else {
				outputDirName = System.getProperty("user.dir");
			}
		}
		
		if (outputDirName != null) {
			File dir = new File(outputDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				setBundlesOutputDir(dir);
				result = dir;
			} else {
				errMsg = String.format("The App config directory '%s' does not exist.", dir.getAbsolutePath());
			}
		} else {
			errMsg = String.format("No command line argument for the App config directory was specified and the system environment variable '%s' is missing.",
					ConfigFinder.CSPACE_JEESERVER_HOME);
		}
		
		if (errMsg != null) {
			log.error(errMsg);
		}
		
		return result;
	}

	public static boolean isFromBuild() {
		return fromBuild;
	}


	public static void setFromBuild(boolean fromBuild) {
		CommandLine.fromBuild = fromBuild;
	}
}
