package org.collectionspace.chain.controller;

/* Invoke from command line with recordtype, domain, maketype (core/delta) and configfile
 * 
 * configfile is the config for the tenant in question.
 * record is the type of the record according to its name in the URL of the service (eg "collectionobjects")
 * domain is the section of the XML to generate the XSD for (eg "collectionspace_core")
 * maketype should be core. The other value, delta, is experimental.
 * 
 * eg java -jar cspace/conf/csmake.jar -c /src/main/resources -o /target/plugins
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.impl.SLF4JLog;
import org.apache.log4j.Level;
import org.collectionspace.chain.csp.config.impl.parser.AssemblingContentHandler;
import org.collectionspace.chain.installation.ServiceConfigGeneration;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.services.common.api.JEEServerDeployment;
import org.collectionspace.services.common.api.CommonAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elca.el4j.services.xmlmerge.config.AttributeMergeConfigurer;
import ch.elca.el4j.services.xmlmerge.config.ConfigurableXmlMerge;
import ch.elca.el4j.services.xmlmerge.Configurer;

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
	private static final Logger logger = LoggerFactory.getLogger(CommandLine.class);
	
	private static final String SERVICE_SCHEMA_VERSION = "4.0";
	private static final String SERVICE_BINDINGS_VERSION = "1.1";
	private static final String SCHEMA_AND_DOCTYPES_DIR = JEEServerDeployment.NUXEO_SERVER_DIR;
	private static final String TENANT_JEESERVER_CONFIG_DIR = "lib";
	private static final String TENANT_BINDINGS_DIR = JEEServerDeployment.CSPACE_DIR_NAME + File.separator
			+ JEEServerDeployment.CONFIG_DIR_PATH + File.separator + JEEServerDeployment.TENANT_BINDINGS_ROOTDIRNAME;

	private static Options options;
	private static org.apache.commons.cli.CommandLine cmd;

	private static final boolean kVALUE_NEEDED = true;
	private static final boolean kNO_VALUE_NEEDED = false;

	private static final String ARG_CONFIG_APPLICATION_BASE_DIR = "ac"; // base directory for the Application layer's configuration files
	private static final String ARG_CONFIG_SERVICE_BASE_DIR = "sc"; // base directory for the Service layer's configuration files.
	private static final String ARG_BASE_OUTPUT_DIR = "o";
	private static final String ARG_SILENT = "s";
	private static final String ARG_HELP = "h";

	private static boolean fromBuild;
	private static File cspaceHomeDir;
	private static File appConfigDir;
	private static File servicesConfigDir;
	private static File bindingsOutputDir;
	private static File bundlesOutputDir;
	private static File baseOutputDir;
	
	private static Map<String, ServiceConfigGeneration> serviceBundlesInfo = new HashMap<String, ServiceConfigGeneration>();

	private static final String XMLMERGE_DEFAULT_PROPS_STR = "matcher.default=ID\n";

	private static final String SERVICES_DELTA_FILE = "tenant-bindings-proto-unified.xml";

	private static final Object LOG4J_FILENAME = "cspace-app-tool.log"; // from log4j.properties file in src/main/resources

	private static void changeLoggerLevel(Level level) {

		if (logger instanceof org.slf4j.impl.Log4jLoggerAdapter) {
			try {
				Class<? extends Logger> loggerIntrospected = logger.getClass();
				Field fields[] = loggerIntrospected.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					String fieldName = fields[i].getName();
					if (fieldName.equals("logger")) {
						fields[i].setAccessible(true);
						org.apache.log4j.Logger loggerImpl = (org.apache.log4j.Logger) fields[i].get(logger);

						loggerImpl.setLevel(level);
						break;
						//	                    if (level == DIAGNOSTIC_LEVEL) {
						//	                        loggerImpl.setLevel(Level.DEBUG);
						//	                    } else {
						//	                        loggerImpl.setLevel(org.apache.log4j.Logger.getRootLogger().getLevel());
						//	                    }

						// fields[i].setAccessible(false);
					}
				}
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(SLF4JLog.class).error(
						"An error was thrown while changing the Logger level", e);
			}
		}

	}

	private static void logFailureAndExit(String failureMessage) {
		String message = "Execution failure: ";
		if (failureMessage == null) {
			failureMessage = "Unknown reason(s).";
		}
		logger.error(message + failureMessage);
		System.exit(-1);
	}

	private static File getCspaceHomeDir() {
		return cspaceHomeDir;
	}

	private static void setCspaceHomeDir(File dir) {
		cspaceHomeDir = dir;
	}

	private static void setAppConfigDir(File dir) {
		appConfigDir = dir;
	}

	private static File getAppConfigDir() {
		return appConfigDir;
	}

	private static void setServicesConfigDir(File dir) {
		servicesConfigDir = dir;
	}

	private static File getServicesConfigDir() {
		return servicesConfigDir;
	}

	public static File getBindingsOutputDir() {
		return bindingsOutputDir;
	}

	private static void setBindingsOutputDir(File dir) {
		bindingsOutputDir = dir;
	}

	private static File getBundlesOutputDir() {
		return bundlesOutputDir;
	}

	private static void setBundlesOutputDir(File dir) {
		bundlesOutputDir = dir;
	}

	private static void setBaseOutputDir(File dir) {
		baseOutputDir = dir;
	}

	private static File getBaseOutputDir() {
		return baseOutputDir;
	}

	/*
	 * Returns the path of the 'CSPACE_JEESERVER_HOME' environment variable. If
	 * the directory refer to by 'CSPACE_JEESERVER_HOME' does not exist, then
	 * this tool quits.
	 */
	private static final String getJEEServerHomePath() {
		String result = null;

		Map<String, String> env = System.getenv();
		String jeeHomeDirPath = env.get(ConfigFinder.CSPACE_JEESERVER_HOME);
		if (jeeHomeDirPath != null && jeeHomeDirPath.trim().isEmpty() == false) {
			result = jeeHomeDirPath;
			File jeeHomeDir = new File(jeeHomeDirPath);
			if (jeeHomeDir.exists() == false || jeeHomeDir.isDirectory() == false) {
				showHelp();
				logFailureAndExit(String.format(
						"The environment variable '%s' refers to a directory that does not exist: '%s'",
						ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDirPath));
			}
		}

		return result;
	}

	private static final FilenameFilter getConfigFileFilter(File configDir) {
		FilenameFilter result = new AppConfigBuildFileFilter();
		setFromBuild(true);

		String jeeHomeDir = getJEEServerHomePath();
		if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
			if (configDir.getAbsolutePath().startsWith(jeeHomeDir) == true) {
				setFromBuild(false);
				result = new AppConfigDeployFileFilter();
			}
		}

		return result;
	}

	/*
	 * Look for and return a read-only configuration directory.
	 */
	private static final File resolveConfigDir(String configDirPath, String configDirLabel, String argConfigDir) {
		return resolveConfigDir(configDirPath, configDirLabel, argConfigDir, null); // Pass a null value for the 'optionalJEESuffix'
	}

	/*
	 * Look for and return a read-only configuration directory.
	 */
	private static final File resolveConfigDir(String configDirPath, String configDirLabel, String argConfigDir,
			String optionalJEESuffix) {
		File result = null;
		String errMsg = null;

		String configDirName = cmd != null ? cmd.getOptionValue(argConfigDir) : null;
		if (configDirName == null || configDirName.isEmpty() == true) {
			// If they didn't specify the config dir as a command line argument
			// then look for the JEE server path in the environment vars and derive the config dir from it
			String jeeHomeDir = getJEEServerHomePath();
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				configDirName = jeeHomeDir;
				if (optionalJEESuffix != null) {
					configDirName = configDirName + File.separator + optionalJEESuffix; // Essentially a special case for the Application layer's config location on a JEE server like Apache Tomcat
				}
				logger.info(String
						.format("No command line argument '-%s' for the %s config directory was specified so we're using the system environment variable '%s'='%s' to locate the config files.",
								argConfigDir, configDirLabel, ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDir));
			}
		}

		if (configDirName != null) {
			if (configDirPath != null && configDirPath.trim().isEmpty() == false) {
				configDirName = configDirName + File.separator + configDirPath;
			}

			File dir = new File(configDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				result = dir;
			} else {
				errMsg = String.format("The %s config directory '%s' does not exist or is not a directory.",
						configDirLabel, dir.getAbsolutePath());
			}
		} else {
			errMsg = String
					.format("No command line argument '-%s' for the %s config directory was specified and the system environment variable '%s' is missing.",
							argConfigDir, configDirLabel, ConfigFinder.CSPACE_JEESERVER_HOME);
		}

		if (errMsg != null) {
			logger.error(errMsg);
		}

		return result;
	}

	/*
	 * Returns the list of tenant configuration files.
	 */
	private static final File[] getListOfTenantConfigFiles(File configDir) {
		File[] result = { new File("") };

		if (configDir.exists() && configDir.isDirectory()) {
			logger.info(String.format("Using the Application layer's configuration files at directory: '%s'",
					configDir.getAbsolutePath()));
			String[] fileNameList = configDir.list(getConfigFileFilter(configDir));
			if (fileNameList != null && fileNameList.length > 0) {
				ArrayList<File> fileList = new ArrayList<File>();
				for (String fileName : fileNameList) {
					File tenantConfigFile = new File(configDir.getAbsolutePath() + "/" + fileName);
					fileList.add(tenantConfigFile);
					logger.info(String.format("Found the Application layer's tenant configuration file: '%s'",
							tenantConfigFile.getAbsolutePath()));
				}
				result = fileList.toArray(result);
			} else {
				logFailureAndExit(String.format(
						"No Application layer tenant configuration files exist in the directory: '%s'",
						configDir.getAbsolutePath()));
			}
		} else {
			logFailureAndExit(String
					.format("The Application layer configuration argument directory '%s' does not exist or is not a directory.",
							configDir.getAbsolutePath()));
		}

		return result;
	}

	private static void showHelp() {
		final String FOOTER_MSG = "\nYou can run this command with no arguments only if you've set the 'CSPACE_JEESERVER_HOME'"
				+ " systen environment variable to point to the directory of the JEE server where CollectionSpace is installed.\n.\n";
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("options", null, getOptions(), FOOTER_MSG);
	}

	private static void showHelp(Exception e) {
		String errMsg = e.getMessage();
		if (errMsg != null) {
			logger.error(errMsg);
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
		newOption = new Option(ARG_CONFIG_APPLICATION_BASE_DIR, kVALUE_NEEDED,
				"Applicaiton layer's base configuration directory.");
		options.addOption(newOption);

		// add option for specifying the Service layer's config base/root directory
		newOption = new Option(ARG_CONFIG_SERVICE_BASE_DIR, kVALUE_NEEDED,
				"Service layer's base configuration directory.");
		options.addOption(newOption);

		// add option for specifying the output directory for the bundles and bindings
		newOption = new Option(
				ARG_BASE_OUTPUT_DIR,
				kVALUE_NEEDED,
				"Output directory for generated document types, schemas, and bindings - <optional> current directory is the default ouput directory.");
		newOption.setRequired(false);
		options.addOption(newOption);

		// add option for specifying a verbose message output
		newOption = new Option(ARG_SILENT, kNO_VALUE_NEEDED,
				"Suppress non-warning and non-error output messages - <optional> default is to output all informational messages.");
		newOption.setRequired(false);
		options.addOption(newOption);

		// add option for help
		newOption = new Option(ARG_HELP, kNO_VALUE_NEEDED, "Shows help");
		options.addOption(newOption);

		CommandLine.options = options;

		return;
	}

	/*
	 * Uses the org.apache.commons.cli package for parsing and handling command
	 * line arguments. See http://commons.apache.org/cli/index.html
	 */
	private static boolean parseArgs(String[] args) {
		boolean result = false;

		if (args.length > 0) {
			try {
				setOptions();
				CommandLineParser parser = new PosixParser();
				cmd = parser.parse(options, args);

				@SuppressWarnings("unchecked")
				List<String> unparsedArgs = (List<String>) cmd.getArgList();
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
			//
			// There were no commandline arugments, so look in system environ for the cspace deployment folder
			//
			String jeeHomeDir = getJEEServerHomePath();
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				File dir = new File(jeeHomeDir);
				if (dir.exists() == true) {
					result = true;
				} else {
					logFailureAndExit(String.format("Missing directory '%s' from system environment variable '%s'.",
							dir.getAbsoluteFile(), jeeHomeDir));
				}
			} else {
				showHelp(new Exception("No arguments specified."));
				logFailureAndExit(String
						.format("To run this command you must supply either command line arguments or set the system environment variable '%s' to the fully qualified base path of the JEE server where CollectionSpace is installed.",
								ConfigFinder.CSPACE_JEESERVER_HOME));
			}
		}

		return result;
	}

	private static InputStream getServiceBindingsDeltaStream() throws Exception {
		InputStream result = null;

		File servicesConfigDir = getServicesConfigDir();
		File servicesDeltaFile = new File(servicesConfigDir + "/" + SERVICES_DELTA_FILE);
		result = new FileInputStream(servicesDeltaFile);

		return result;
	}

	private static String mergeWithServiceDelta(String generatedBindings) throws Exception {
		String result = null;

		InputStream generatedBindingsStream = new ByteArrayInputStream(generatedBindings.getBytes(StandardCharsets.UTF_8));
		InputStream serviceBindingsDeltaStream = getServiceBindingsDeltaStream();
		InputStream[] inputStreams = { generatedBindingsStream, serviceBindingsDeltaStream };

		Configurer configurer = new AttributeMergeConfigurer();
		InputStream mergedStream = new ConfigurableXmlMerge(configurer).merge(inputStreams);
		result = IOUtils.toString(mergedStream, "UTF-8");

		return result;
	}

	private static final void writeToFile(String outputFileName, String outputString) {
		File outputFile = new File(outputFileName);

		try {
			FileUtils.writeStringToFile(outputFile, outputString);
		} catch (Exception e) {
			logger.debug(String.format("Could not write service bindings file to: %s", outputFile), e);
		}
	}

	/*
	 * Resolve where we're going to write out all the generate doctype bundles,
	 * schema bundles, and service bindings.
	 */
	private static final void setOutputDirectories() {
		//
		// Make sure we have a base directory to output the generated schema and document type bundles.
		//
		File baseOutputDir = resolveOutputDir((String) null, "Service artifacts", ARG_BASE_OUTPUT_DIR);
		if (baseOutputDir != null) {
			setBaseOutputDir(baseOutputDir);
		} else {
			logFailureAndExit(null);
		}

		//
		// Resolve where we're going to write out the Nuxeo bundle (doctypes and schema) files.
		//		
		File bundlesOutputDir = resolveOutputDir(getBaseOutputDir(), JEEServerDeployment.NUXEO_SERVER_PLUGINS_DIR,
				"Service Bundles");
		if (bundlesOutputDir != null) {
			setBundlesOutputDir(bundlesOutputDir);
		} else {
			logFailureAndExit(null);
		}

		//
		// Resolve where we're going to write out the bindings files.
		//
		File bindingsOutputDir = resolveOutputDir(getBaseOutputDir(), TENANT_BINDINGS_DIR, "Service Bindings");
		if (bindingsOutputDir != null) {
			setBindingsOutputDir(bindingsOutputDir);
		} else {
			logFailureAndExit(null);
		}
	}
	
	private static final void dumpServiceArtifactMetadata(File tenantConfigFile, ServiceConfigGeneration xsdMetadata) throws Exception {
		if (logger.isDebugEnabled() == true) {
			logger.debug(String.format("Schema types defined in config: %s", tenantConfigFile.getAbsolutePath()));
			Map<String, String> xsdschemas = xsdMetadata.getDefinedSchemas();
			for (String schemaName : xsdschemas.keySet()) {
				logger.debug(String.format("\tSchema file name: %s", schemaName));
			}

			Map<String, String> doctypes = xsdMetadata.getServiceDoctypeBundles();
			logger.debug(String.format("Document types defined in config: %s", tenantConfigFile.getAbsolutePath()));
			for (String doctypeName : doctypes.keySet()) {
				logger.debug(String.format("\tDocument type file name: %s", doctypeName));
			}
		}
	}
	
	/*
	 * Creates and writes out the Service bundles -i.e., the Nuxeo doctypes and Nuxeo schemas.
	 */
	private static final String generateServiceBundles(File tenantConfigFile) {
		String result = null;
		
		//
		// Generate all the Service schemas from the Application layer's configuration records
		//
		ServiceConfigGeneration xsdMetadata = null;
		try {
			xsdMetadata = new ServiceConfigGeneration(serviceBundlesInfo, tenantConfigFile, CommonAPI.GENERATE_BUNDLES, SERVICE_SCHEMA_VERSION, getBundlesOutputDir(),
					SERVICE_BINDINGS_VERSION, getBindingsOutputDir());
			serviceBundlesInfo.put(tenantConfigFile.getName(), xsdMetadata);
			dumpServiceArtifactMetadata(tenantConfigFile, xsdMetadata); // debugging output
		} catch (Exception e) {
			result = String.format("Error encountered generating service bindings for '%s' tenant configuration.",
					tenantConfigFile.getAbsolutePath());
			logger.error(result, e);
		}
		
		return result;
	}
	
	/*
	 * Creates and writes out the Service layer's tenant bindings for a given tenant config file.
	 */
	private static final String generateServiceBindings(File tenantConfigFile) {
		String result = null;
		
		ServiceConfigGeneration tenantBindingsMetadata = null;
		try {
			tenantBindingsMetadata = new ServiceConfigGeneration(serviceBundlesInfo, tenantConfigFile, CommonAPI.GENERATE_BINDINGS, SERVICE_SCHEMA_VERSION, null,
					SERVICE_BINDINGS_VERSION, getBindingsOutputDir());
			String tenantName = tenantBindingsMetadata.getSpec().getAdminData().getTenantName();

			String tenantBindings = tenantBindingsMetadata.getTenantBindings();
			if (tenantBindings != null) {
				if (logger.isTraceEnabled()) {logger.trace(String.format("Service Bindings Begin: %s >>>+++++++++++++++++++++++++++++++++>>>",
							tenantConfigFile.getName()));
					logger.trace(tenantBindings);
					logger.trace("Service Bindings End: <<<+++++++++++++++++++++++++++++++++<<<");
				}
			} else {
				throw new Exception(String.format("Could not create tenant bindings for file %s",
						tenantConfigFile));
			}

			String mergedTenantBindings = mergeWithServiceDelta(tenantBindings);
			if (mergedTenantBindings != null) {
				String mergedTenantBindingsFilename = getBindingsOutputDir().getAbsolutePath() + File.separator
						+ tenantName + "-" + JEEServerDeployment.TENANT_BINDINGS_PROTOTYPE_FILENAME;
				FileUtils.writeStringToFile(new File(mergedTenantBindingsFilename), mergedTenantBindings);
				if (logger.isInfoEnabled()) {
					logger.info("***");
					logger.info(String.format("Config Generation: '%s' - Wrote merged tenant bindings to: '%s'", 
							tenantConfigFile.getName(), mergedTenantBindingsFilename));
				}
			} else {
				throw new Exception(String.format("Config Generation: '%s' - Could not create merged tenant bindings for file %s",
						tenantConfigFile.getName(), tenantConfigFile));
			}
		} catch (Exception e) {
			result = String.format("Config Generation: '%s' - Error encountered generating service bindings for tenant '%s' configuration.",
					tenantConfigFile.getName(), tenantConfigFile.getAbsolutePath());
			logger.error(result, e);
		}
		
		return result;
	}
	
	private static final void setLoggingLevel() {
		//
		// By default, we'll provide verbose messages to the user
		//
		changeLoggerLevel(Level.INFO);
		
		//
		// If the user used the '-s' command line argument then we switch to "silent" or "suppress" messages mode.
		//
		if (cmd != null) {
			if (cmd.hasOption(ARG_SILENT) == true) { // If we find "-s' on the command line then we need to set quiet the output to ERROR level only
				changeLoggerLevel(Level.ERROR);
			}
		}
	}

	public static final void main(String[] args) throws Exception {
		//
		// Parse the commandline arguments.
		//
		boolean parseSuccessful = parseArgs(args);
		if (parseSuccessful == false) {
			logFailureAndExit("Could not sucessfully parse the command line arguments. See message above.");
		}
		//
		// Set our logging/output level
		//
		setLoggingLevel();

		//
		// Find the Services' configuration base directory
		//
		File serviceConfigDir = resolveConfigDir(TENANT_BINDINGS_DIR, "Services", ARG_CONFIG_SERVICE_BASE_DIR);
		if (serviceConfigDir != null) {
			setServicesConfigDir(serviceConfigDir);
		} else {
			logFailureAndExit(null);
		}
		logger.info(String.format("Using the Service layer's configuration files at directory: '%s'.",
				serviceConfigDir.getAbsolutePath()));

		//
		// Find the Application layer's configuration base directory
		//
		File tenantConfigDir = resolveConfigDir("", "Application", ARG_CONFIG_APPLICATION_BASE_DIR,
				TENANT_JEESERVER_CONFIG_DIR);
		if (tenantConfigDir != null) {
			setAppConfigDir(tenantConfigDir);
		} else {
			logFailureAndExit(null);
		}
		
		//
		// Start processing the Application layer's configuration so we can generate the corresponding
		// Service layer's artifacts -i.e., the Nuxeo doctypes, Nuxeo schemas, and the service bindings.
		//
		File[] tenantConfigFileList = getListOfTenantConfigFiles(getAppConfigDir());
		//
		// Setup the output directories
		//
		setOutputDirectories();
		//
		// Iterate over each tenant configuration file and create the corresponding bindings.
		//
		String errMsg = null;
		for (File tenantConfigFile : tenantConfigFileList) {
			String logMsg = String.format("Config Generation: '%s' - ### Started processing tenant configuration file '%s'.", 
					tenantConfigFile.getName(), tenantConfigFile.getAbsolutePath());
			logger.info("###");
			logger.info(logMsg);
			logger.info("###");
			
			//
			// First generate the Service bundles -i.e., the Nuxeo doctype and schema bundles
			//
			errMsg = generateServiceBundles(tenantConfigFile);
			if (errMsg != null  && errMsg.isEmpty() == false) {
				logFailureAndExit(errMsg);
			}				
			//
			// Now generate the service bindings
			//
			errMsg = generateServiceBindings(tenantConfigFile);
			if (errMsg != null  && errMsg.isEmpty() == false) {
				logFailureAndExit(errMsg);
			}
			
			logMsg = String.format("Config Generation: '%s' - ### Finished processing tenant configuration file '%s'.", 
					tenantConfigFile.getName(), tenantConfigFile.getAbsolutePath());
			logger.info(logMsg);
			logger.info("***\n");
		}

		//
		// We made it!
		//
		String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
		logger.info("Config Generation - Execution complete.");
		logger.info("###");
		logger.info(String.format("### - Check the log file at %s: '%s' for 'ERROR' messages.", currentPath, LOG4J_FILENAME));
		logger.info("###");
		logger.info(String.format("Service artifacts written out to '%s'.", getBaseOutputDir().getAbsolutePath()));
		logger.info(String.format("Temporary XMLMerge files were written out to '%s'.", AssemblingContentHandler.getTempDirectory()));
	}

	/*
	 * Will try to either find or create an output directory for a set of Service artifacts.
	 */
	private static File resolveOutputDir(File baseDir, String outputDirPath, String outputDirLabel) {
		File result = null;

		String outputDirName = baseDir.getAbsolutePath();
		if (outputDirPath != null && outputDirPath.trim().isEmpty() == false) {
			outputDirName = outputDirName + File.separator + outputDirPath;
		}

		if (outputDirName != null) {
			File dir = new File(outputDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				result = dir;
			} else {
				//
				// The output directory does not exist, so try to create it
				//
				boolean createdDir = false;
				try {
					createdDir = dir.mkdirs();
				} catch (SecurityException Se) {
					logger.trace(Se.getMessage(), Se);
				}

				if (createdDir == true) {
					result = dir;
					logger.debug(String.format("Created directory '%s' for %s.", dir.getAbsolutePath(), outputDirLabel));
				} else {
					logFailureAndExit(String.format("Could not create output directory '%s' for %s.",
							dir.getAbsolutePath(), outputDirPath));
				}
			}
		}

		return result;
	}

	/*
	 * Resolve the location for an output directory. Create one based on the
	 * "current directory" if the user did not specify one as a command line
	 * argument.
	 */
	private static File resolveOutputDir(String outputDirPath, String outputDirLabel, String argOuputDir) {
		File result = null;
		String errMsg = null;

		String outputDirName = cmd != null ? cmd.getOptionValue(argOuputDir) : null;
		if (outputDirName == null || outputDirName.isEmpty() == true) {
			// If they didn't specify the output dir as a command line argument
			// then look for the JEE server path in the environment vars and derive the output dir from it
			String jeeHomeDir = getJEEServerHomePath();
			if (jeeHomeDir != null && jeeHomeDir.trim().isEmpty() == false) {
				outputDirName = jeeHomeDir;
				if (outputDirPath != null && outputDirPath.trim().isEmpty() == false) {
					outputDirName = outputDirName + "/" + outputDirPath;
				}
				logger.info(String.format(
						"No command line argument '-%s' for the output directory of the %s was specified so we're using "
								+ "the system environment variable '%s'='%s' to determine the output directory.",
						argOuputDir, outputDirLabel, ConfigFinder.CSPACE_JEESERVER_HOME, jeeHomeDir));
			} else {
				outputDirName = System.getProperty("user.dir");
				if (outputDirPath != null && outputDirPath.trim().isEmpty() == false) {
					outputDirName = outputDirName + outputDirPath;
				}
				logger.warn(String
						.format("No command line argument '-%s' for the output directory of the %s was specified and the system environment variable '%s' is missing.  Using the current directory instead.",
								argOuputDir, outputDirLabel, ConfigFinder.CSPACE_JEESERVER_HOME));
				logger.debug(String.format("Will attempt to write '%s' out to current system directory at '%s'",
						outputDirLabel, outputDirName));
			}
		}

		if (outputDirName != null) {
			File dir = new File(outputDirName);
			if (dir.exists() == true && dir.isDirectory() == true) {
				result = dir;
			} else {
				//
				// The output directory does not exist, so try to create it
				//
				boolean createdDir = false;
				try {
					createdDir = dir.mkdirs();
				} catch (SecurityException Se) {
					logger.trace(Se.getMessage(), Se);
				}
				if (createdDir == true) {
					result = dir;
					logger.debug(String.format("Created directory '%s' for %s.", dir.getAbsolutePath(), outputDirLabel));
				} else {
					errMsg = String.format("Could not create output directory '%s' for %s.", dir.getAbsolutePath(),
							outputDirPath);
				}
			}
		}

		if (errMsg != null) {
			logFailureAndExit(errMsg);
		} else {
			logger.info(String.format("Writing %s out to directory %s.", outputDirLabel, result.getAbsolutePath()));
		}

		return result;
	}

	/*
	 * Returns 'true' if we're using configuration files from the Application
	 * layer's source tree as opposed to those from the JEE server directory;
	 * e.g., the "tomcat/lib" directory.
	 */
	public static boolean isFromBuild() {
		return fromBuild;
	}

	public static void setFromBuild(boolean fromBuild) {
		CommandLine.fromBuild = fromBuild;
	}
}
