package org.collectionspace.chain.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.persistence.services.ServicesStorageGenerator;
import org.collectionspace.chain.csp.persistence.services.TenantSpec;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.csp.helper.test.TestConfigFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


public class XsdGeneration {
	private static final Logger log = LoggerFactory.getLogger(XsdGeneration.class);
	
	private static final String EOL = "\r\n"; // <CR><LF>
	private static final int MAX_CONFIG_FILE_SIZE = 100 * 1024;
	
	private static final String NUXEO_PLUGIN_TEMPLATES_DIR = "nx-plugin-templates";
	private static final String NUXEO_SCHEMA_TYPE_TEMPLATES_DIR = NUXEO_PLUGIN_TEMPLATES_DIR + "/" + "schema-type-templates";
	private static final String NUXEO_DOCTYPE_TEMPLATES_DIR = NUXEO_PLUGIN_TEMPLATES_DIR + "/" + "doc-type-templates";
	private static final String DOCTYPE_TEMPLATES_DIR = "doc-type-templates";
	private static final String SCHEMATYPE_TEMPLATES_DIR = "schema-type-templates";
	
	private static final String DEFAULT_PARENT_TYPE = "CollectionSpaceDocument";
	private static final String SCHEMA_PARENT_TYPE_VAR = "$SchemaParentType}";
	private static final String COLLECTIONSPACE_CORE_SCHEMA_NAME = "collectionspace_core";
	private static final String DEFAULT_BUNDLE_PREAMBLE = "collectionspace";
	private static final String SCHEMA_BUNDLE_QUALIFIER = "schema";
	private static final String DOCTYPE_BUNDLE_QUALIFIER = "doctype";
	private static final String SHARED_QUALIFIER = "shared";
		
	private static final String SCHEMA_NAME_VAR = "${SchemaName}";
	private static final String SCHEMA_NAME_LOWERCASE_VAR = "${SchemaName_LowerCase}";
	
	private static final String DOCTYPE_NAME_VAR = "${NuxeoDocTypeName}";
	private static final String DOCTYPE_NAME_LOWERCASE_VAR = "${NuxeoDocTypeName_LowerCase}";

	private static final String SERVICE_NAME_VAR = "${ServiceName}";
	private static final String SERVICE_NAME_LOWERCASE_VAR = "${ServiceName_LowerCase}";
	
	private static final String REQUIRE_BUNDLE_LIST_VAR = "${Require-Bundle-List}";
	private static final String SCHEMA_ELEMENTS_LIST_VAR = "${SchemaElements}";
	private static final String EMC_LAYOUT_LIST_VAR = "${LayoutList}";

	private static final String SCHEMA_ELEMENT_TEMPLATE = "<schema name=\"" + SCHEMA_NAME_VAR + "\" />";	
	private static final String META_INF_DIR = "META-INF";
	private static final String MANIFEST_FILE = "MANIFEST.MF";
	private static final String OSGI_INF_DIR = "OSGI-INF";
	private static final String SCHEMAS_DIR = "schemas";
	private static final String JAR_EXT = ".jar";	

	private static final String TENANT_QUALIFIER = "Tenant";

	private static final String NX_PLUGIN_NAME = "nx_plugin_out";
	
	private HashMap<String, String> serviceSchemas = new HashMap<String, String>();
	private HashMap<String, String> serviceSchemaBundles = new HashMap<String, String>();
	private HashMap<String, String> serviceDoctypeBundles = new HashMap<String, String>();
	
	private String tenantBindings = null;
	
	public HashMap<String, String> getServiceSchemas() {
		return serviceSchemas;
	}
	
	public HashMap<String, String> getServiceSchemaBundles() {
		return serviceSchemaBundles;
	}
	
	public HashMap<String, String> getServiceDoctypeBundles() {
		return serviceDoctypeBundles;
	}
	
	public XsdGeneration(String configfile, String recordType, String domain, String type, String schemaVersion) throws Exception {

		String current = null;
		        
		CSPManager cspm=getServiceManager(configfile);
		Spec spec = getSpec(cspm);
		PrintStream out = System.out;
		for (Record record : spec.getAllRecords()) {
			if (record.getServicesCmsType().equalsIgnoreCase("default") == true) {
				out.println("++++++++++++++++++++++++++++++++++++++++++++++++++");
				out.println(String.format("getID = %s", record.getID()));
				out.println(String.format("getServicesCmsType = %s", record.getServicesCmsType()));
				out.println(String.format("getServicesType = %s", record.getServicesType()));
				out.println(String.format("getServicesURL = %s", record.getServicesURL()));
				out.println(String.format("getServicesTenantSg = %s", record.getServicesTenantSg()));
				out.println(String.format("getServicesTenantPl = %s", record.getServicesTenantPl()));
				out.println(String.format("getServicesTenantAuthSg = %s", record.getServicesTenantAuthSg()));
				out.println(String.format("getServicesTenantAuthPl = %s", record.getServicesTenantAuthPl()));
				out.println(String.format("getServicesRecordPath = %s", record.getServicesRecordPath("")));
				
				out.println("getServicesRecordPaths =");
				String[] recordPathList = record.getServicesRecordPaths();
				for (String recordPath : recordPathList) {
					out.println(String.format("\t%s", recordPath));
				}
				
				out.println(String.format("getServicesInstancesPath = %s", record.getServicesInstancesPath()));
				out.println(String.format("getServicesSingleInstancePath = %s", record.getServicesSingleInstancePath()));
				
				out.println("getServicesInstancesPaths =");			
				String recordServicesInstancesPathList[] = record.getServicesInstancesPaths();
				if (recordServicesInstancesPathList != null && recordServicesInstancesPathList.length > 0) {
					for (String path : recordServicesInstancesPathList) {
						out.println(String.format("\t%s", path));
					}
				} else {
					out.println("\tnull");
				}
			
				
				out.println(String.format("getServicesAbstractCommonList = %s", record.getServicesAbstractCommonList()));
				out.println(String.format("getServicesValidatorHandler = %s", record.getServicesValidatorHandler()));
				out.println(String.format("getServicesCommonList = %s", record.getServicesCommonList()));
				out.println(String.format("getServicesSchemaBaseLocation = %s", record.getServicesSchemaBaseLocation()));
				out.println(String.format("getServicesDocHandler = %s", record.getServicesDocHandler()));
				out.println(String.format("getServicesListPath = %s", record.getServicesListPath()));
				out.println(String.format("getServicesFieldsPath = %s", record.getServicesFieldsPath()));
				out.println(String.format("getServicesSearchKeyword = %s", record.getServicesSearchKeyword()));
				
				out.println("##################################################");
			}
		}
		
		// 1. Setup a hashmap to keep track of which records and bundles we've already processed.
		// 2. Loop through all the known record types
		// 3. We could create a Nuxeo bundle for each new schema type
		// 4. We could then create a new Nuxeo bundle for each new document type
		
		if (spec.hasRecordByServicesUrl(recordType)) {			
			Record record = spec.getRecordByServicesUrl(recordType);
			if (type.equals("core")) { // if 'true' then generate an XML Schema .xsd file
				if (record.isGenerateServicesSchema() == false) {
					log.debug(String.format("The application config record '%s' is already configured in the Services layer and will be skipped.",
							record.getID()));
				} else {
					MakeXsd catlog = new MakeXsd(getTenantData(cspm));
					HashMap<String, String> definedSchemaList = catlog.getDefinedSchemas(
							record, recordType, schemaVersion);
					// For each schema defined in the configuration record, check to see if it was already
					// defined in another record.
					for (String definedSchema : definedSchemaList.keySet()) {
						if (getServiceSchemas().containsKey(definedSchema) == false) {
							// If the newly defined schema doesn't already exist in our master list then add it
							try {
								createSchemaBundle(record, definedSchema, definedSchemaList);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							getServiceSchemas().put(definedSchema, definedSchemaList.get(definedSchema));
							log.debug(String.format("New Services schema '%s' defined in Application configuration record '%s'.",
									definedSchema, record.getID()));
						} else {
							// Otherwise, it already exists so emit a warning just in case it shouldn't have been redefined.
							log.warn(String.format("Services schema '%s' defined in record '%s', but was previously defined in another record.",
									definedSchema, record.getID()));
							log.trace(String.format("Redefined services schema is: %s", definedSchemaList.get(definedSchema)));
						}
					}
					//
					// Create the Nuxeo bundle document type
					//
					createDocumentTypeBundle(record, definedSchemaList);
				}
			} else if (type.equals("delta")) { // Create the service bindings.
				Services tenantbob = new Services(getSpec(cspm), getTenantData(cspm),false);
				tenantBindings = tenantbob.doit();
			}
		} else {
			log.error(String.format("Record type '%s' is unknown in configuration file", configfile));
		}
	}
	
	private String getRequiredBundlesList(Record record, HashMap<String, String> definedSchemaList) throws Exception {
		String result = null;
		StringBuffer strBuf = new StringBuffer();
		final String lineSeparator = "," + EOL;
		
		for (String schemaName : definedSchemaList.keySet()) {
			String bundleName = getServiceSchemaBundles().get(schemaName);
			if (bundleName != null && bundleName.isEmpty() == false) {
				bundleName = FilenameUtils.removeExtension(bundleName); // strip off the .jar file extension
				log.debug(String.format("Included in Require-Bundle list entry: '%s'", bundleName));
				strBuf.append(bundleName);
				strBuf.append(lineSeparator);
			} else {
				String errMsg = String.format("The bundle file definition for the schema '%s' could not be found.", schemaName);
				log.error(errMsg);
				throw new Exception(errMsg);
			}			
		}

		int len = strBuf.length();
		if (len > 0) {
			result = strBuf.substring(0, len - lineSeparator.length()); // Remove the last/extra line separator
		}
		
		return result;
	}
	
	private String getSchemaElementsList(Record record, HashMap<String, String> definedSchemaList) throws Exception {
		String result = null;
		StringBuffer strBuf = new StringBuffer();
		
		String tabs = "";
		for (String schemaNameWithExtension : definedSchemaList.keySet()) {
			String schemaName =	FilenameUtils.removeExtension(schemaNameWithExtension);
			String element = SCHEMA_ELEMENT_TEMPLATE.replace(SCHEMA_NAME_VAR, schemaName);
			strBuf.append(tabs + element + EOL);
			if (tabs.length() == 0) {
				tabs = "\t\t\t";
			}
		}
		//
		// Remove the last/extra line-return
		//
		int len = strBuf.length();
		if (len > 0) {
			result = strBuf.substring(0, len - EOL.length());
		}
		
		return result;
	}
	
	private void createDocumentTypeBundle(Record record, HashMap<String, String> definedSchemaList) throws Exception {
		// Create a new zip/jar for the doc type bundle
		// Create the META-INF folder and manifest file
		// Create the OSGI-INF folder and process the template files
		String serviceName = record.getServicesTenantSg();
		String tenantName = record.getSpec().getAdminData().getTenantName();
		String docTypeName = record.getServicesTenantSg();

		String tenantQualifier = SHARED_QUALIFIER;
		if (record.isServicesExtension() == true) {
			tenantQualifier = tenantName;
			docTypeName = docTypeName + TENANT_QUALIFIER + record.getSpec().getTenantID();
		}
		String bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + serviceName.toLowerCase()
			+ "." + DOCTYPE_BUNDLE_QUALIFIER + "." + tenantQualifier + "." + docTypeName + JAR_EXT;
		
		if (getServiceDoctypeBundles().containsKey(docTypeName) == false) {
			File doctypeTemplatesDir = new File(NUXEO_DOCTYPE_TEMPLATES_DIR);
			if (doctypeTemplatesDir.exists() == true) {
				log.debug(String.format("### Creating Nuxeo document type ${NuxeoDocTypeName}='%s' in bundle: '%s'",
						docTypeName, bundleName));
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						bundleName));
				//
				// Create the manifest file from the doctype template
				//
				File metaInfTemplate = new File(NUXEO_DOCTYPE_TEMPLATES_DIR + "/" + META_INF_DIR + "/" + MANIFEST_FILE);				
				// Setup the hash map for the variable substitutions
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				substitutionMap.put(SERVICE_NAME_VAR, serviceName);
				substitutionMap.put(SERVICE_NAME_LOWERCASE_VAR, serviceName.toLowerCase());
				substitutionMap.put(DOCTYPE_NAME_VAR, docTypeName);
				substitutionMap.put(DOCTYPE_NAME_LOWERCASE_VAR, docTypeName.toLowerCase());
				substitutionMap.put(EMC_LAYOUT_LIST_VAR, ""); // Once we re-enable the Nuxeo DM webapp, this should be list of layout definitions
				String requiredBundleList = getRequiredBundlesList(record, definedSchemaList);
				substitutionMap.put(REQUIRE_BUNDLE_LIST_VAR, requiredBundleList);
				createManifestFile(metaInfTemplate, substitutionMap, zos);
				//
				// Next, crate the OSGI-INF files
				//
				String schemaElementsList = getSchemaElementsList(record, definedSchemaList);
				substitutionMap.put(SCHEMA_ELEMENTS_LIST_VAR, schemaElementsList);
				
				String schemaParentType = DEFAULT_PARENT_TYPE;
				if (record.isServicesExtension() == true) { // If we know this is an extension of a common base type, we need to set the parent type
					schemaParentType = serviceName;
				}
				substitutionMap.put(SCHEMA_PARENT_TYPE_VAR, schemaParentType);
				createOsgiInfFiles(doctypeTemplatesDir, substitutionMap, zos);
				//
				// We're finished adding entries so close the zip output stream
				// 
				zos.close();
				//
				// Keep track of the doctype bundles we've created
				//
				getServiceDoctypeBundles().put(docTypeName, bundleName);
			}
		} else {
			log.warn(String.format("Nuxeo document type '%s' already exists.  Skipping creation.", docTypeName));
		}
	}

	private void createOsgiInfFiles(File osgiInfTemplatesDir, HashMap<String, String> substitutionMap, ZipOutputStream zos) throws Exception {
		// Check that the OSGI-INF directory exists
		File osgiDir = new File(osgiInfTemplatesDir.getAbsolutePath() + "/" + OSGI_INF_DIR);
		if (osgiDir.exists() == false || osgiDir.isDirectory() == false) {
			throw new Exception(String.format("The %s directory '%s' is missing.", OSGI_INF_DIR, osgiDir.getAbsolutePath()));
		}
		// Create the "OSGI-INF" directory in the output jar/zip file
		zos.putNextEntry(new ZipEntry(OSGI_INF_DIR + "/"));
		zos.closeEntry();
		
		// For each file in the OSGI-INF directory, process it and add a zip/jar entry
		for (File osgiFile : osgiDir.listFiles()) {
			if (osgiFile.isDirectory() == false) {
				zos.putNextEntry(new ZipEntry(OSGI_INF_DIR + "/" + osgiFile.getName()));		
				processTemplateFile(osgiFile, substitutionMap, zos);	
				zos.closeEntry();
			} else {
				System.err.println(String.format("Ignoring directory '%s' while processing OSGI-INF files.", osgiFile.getAbsolutePath()));
			}
		}
	}
	
	/*
	 * Creates an entry in the zip/jar bundle for XML Schema (.xsd) files.
	 */
	private void createSchemaFiles(File osgiInfTemplatesDir,
			HashMap<String, String> substitutionMap,
			ZipOutputStream zos,
			String schemaName, HashMap<String,
			String>definedSchemaList) throws Exception {
		String contentString = definedSchemaList.get(schemaName);
		
		if (contentString != null && contentString.isEmpty() == false) {
			// Create the "schemas" directory in the output jar/zip file
			zos.putNextEntry(new ZipEntry(SCHEMAS_DIR + "/"));
			zos.closeEntry();
			//
			// Create an entry in the zip/jar for the schema file and write it out
			//
			zos.putNextEntry(new ZipEntry(SCHEMAS_DIR + "/" + schemaName));
			zos.write(contentString.getBytes(), 0, contentString.getBytes().length);
			zos.closeEntry();
		} else {
			String errMsg = String.format("The contents for the schema '%s' is empty or missing.", schemaName);
			log.error(errMsg);
			throw new Exception(errMsg);
		}
	}
	
	private void createManifestFile(File metaInfTemplate, HashMap<String, String> substitutionMap, ZipOutputStream zos) throws Exception {
		if (metaInfTemplate.exists() == false) {
			throw new Exception(String.format("The manifest template file '%s' is missing:", metaInfTemplate.getAbsolutePath()));
		}
		// Create the "META-INF" directory in the output jar/bundle zip file
		zos.putNextEntry(new ZipEntry(META_INF_DIR + "/"));
		zos.closeEntry();
		//
		// Now create the MANIFEST.MF file
		//
		zos.putNextEntry(new ZipEntry(META_INF_DIR + "/" + MANIFEST_FILE));		
		// Process the template by performing the variable substitutions
		processTemplateFile(metaInfTemplate, substitutionMap, zos);				
		zos.closeEntry();		
	}
	
	private void createSchemaBundle(Record record, String schemaName, HashMap<String, String>definedSchemaList) throws Exception {
		String serviceName = record.getServicesTenantSg();
		String schemaNameNoFileExt = schemaName.split("\\.")[0]; // Assumes a single '.' in a file name like "foo.xsd"
		String tenantName = record.getSpec().getAdminData().getTenantName();
		// Figure out what the bundle name should be.
		String bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + serviceName.toLowerCase()
				+ "." + SCHEMA_BUNDLE_QUALIFIER + "." + schemaNameNoFileExt;
		if (isGlobalSchema(schemaNameNoFileExt) == true) {
			bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + SHARED_QUALIFIER + "." + SCHEMA_BUNDLE_QUALIFIER + "." + schemaNameNoFileExt;
		}
		bundleName = bundleName + JAR_EXT;
		//
		// Before creating a new bundle, make sure we haven't already created a bundle for this schema extension.
		//
		if (getServiceSchemaBundles().containsKey(schemaName) == false) {
			File schemaTypeTemplatesDir = new File(NUXEO_SCHEMA_TYPE_TEMPLATES_DIR);
			if (schemaTypeTemplatesDir.exists()) {
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						bundleName));
				//
				// Create the manifest file from the schema type template
				//
				File metaInfTemplate = new File(NUXEO_SCHEMA_TYPE_TEMPLATES_DIR + "/" + META_INF_DIR + "/" + MANIFEST_FILE);
				// Setup the hash map for the variable substitutions
				String serviceNameVar = serviceName;				
				if (isGlobalSchema(schemaNameNoFileExt) == true) {
					serviceNameVar = SCHEMA_BUNDLE_QUALIFIER;
				}
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				substitutionMap.put(SERVICE_NAME_VAR, serviceNameVar);
				substitutionMap.put(SERVICE_NAME_LOWERCASE_VAR, serviceNameVar.toLowerCase());
				substitutionMap.put(SCHEMA_NAME_VAR, schemaNameNoFileExt);
				substitutionMap.put(SCHEMA_NAME_LOWERCASE_VAR, schemaNameNoFileExt.toLowerCase());
				createManifestFile(metaInfTemplate, substitutionMap, zos);
				//
				// Next, crate the OSGI-INF files
				//
				createOsgiInfFiles(schemaTypeTemplatesDir, substitutionMap, zos);
				//
				// Finally, create the "schemas" directory entry and add the schema file to it
				//
				createSchemaFiles(schemaTypeTemplatesDir, substitutionMap, zos, schemaName, definedSchemaList);
				//
				// We're finished adding entries so close the zip output stream
				//
				zos.close();
				//
				// Keep track of the schema bundles we've created
				//
				getServiceSchemaBundles().put(schemaName, bundleName);
			} else {
				log.error(String.format("The '%s' directory is missing looking for it at '%s'.", NUXEO_SCHEMA_TYPE_TEMPLATES_DIR, schemaTypeTemplatesDir.getAbsolutePath()));
			}
		} else {
			log.warn(String.format("Nuxeo schema '%s' is being redefined in record '%s'.  Ignoring redefinition and *not* creating a new extension point bundle for the schema.",
					schemaName, record.getID()));
		}
	}


	/*
	 * Try to determine if the schema is global/shared across tenants.
	 */
	private boolean isGlobalSchema(String schemaName) {
		boolean result = false;

		/*
		 * For now, just see if it is the "collectionspace_core" schema, but we'll need a more
		 * general solution in the future.
		 */
		if (schemaName.equalsIgnoreCase(COLLECTIONSPACE_CORE_SCHEMA_NAME) == true) {
			result = true;
		}

		return result;
	}
	
	private static String processTemplateFile(File templateFile,
			HashMap<String, String> substitutionMap,
			ZipOutputStream zos) throws Exception {
		String result = null;
		//
		// Read the entire template file into a memory buffer
		//
		byte[] buf = new byte[MAX_CONFIG_FILE_SIZE];		
		FileInputStream fis = new FileInputStream(templateFile.getAbsoluteFile()); // Read in the template
		int len = 0;
		int fileSize = 0;
		int bufferExceeded = 0;
		while ((len = fis.read(buf)) > 0) {  // WARNING: The whole file needs to fit into our buffer or we fail.
			bufferExceeded++;
			fileSize = fileSize + len;
		}
		fis.close();
		//
		// Peform the variable substitution
		//
		if (bufferExceeded == 1) { // "== 1" means we fit the entire template file into memory.  "> 1" means the template file was too large. "== 0" means the file was missing or empty
			String contentStr = new String(buf, 0, fileSize);
			for (String key : substitutionMap.keySet()) {
				String varReplacementStr = substitutionMap.get(key);
				contentStr = contentStr.replace(key, varReplacementStr);
				System.out.println(String.format("Replacing the string '%s' with '%s'", key, varReplacementStr));
			}
			// write the processed file out to the zip entry
			zos.write(contentStr.getBytes(), 0, contentStr.getBytes().length);
			result = contentStr;
		} else {
			String errMsg = String.format("The file '%s' was empty or missing.", templateFile.getAbsoluteFile());
			if (bufferExceeded > 1) {
				errMsg = String.format("The file '%s' was too large to fit in our memory buffer.  It needs to be less than %d bytes, but was at least %d bytes in size.",
						templateFile.getAbsoluteFile(), MAX_CONFIG_FILE_SIZE, fileSize);
			}
			throw new Exception(errMsg);
		}
		
		log.debug(String.format("The processed file is:\n%s", result));
		return result;
	}
	
	private InputSource getSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream(fallbackFile);
		} catch (CSPDependencyException e) {
			String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
			return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
		}
	}
	
	private Spec getSpec(CSPManager cspm){
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		return spec;
	}

	private TenantSpec getTenantData(CSPManager cspm) {
		
		ServicesStorageGenerator gen=(ServicesStorageGenerator)cspm.getStorage("service");
		TenantSpec td = gen.getTenantData();
		return td;
	}
	
	private CSPManager getServiceManager(String filename) {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		try {
			cspm.go();
			cspm.configure(getSource(filename),new ConfigFinder(null));
		} catch (CSPDependencyException e) {
			log.error("CSPManagerImpl failed");
			log.error(e.getLocalizedMessage() );
		}
		return cspm;
		
	}
}
