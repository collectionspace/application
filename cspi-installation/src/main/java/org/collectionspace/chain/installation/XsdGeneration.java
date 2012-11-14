package org.collectionspace.chain.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
	
	private static final int MAX_CONFIG_FILE_SIZE = 100 * 1024;
	
	private static final String NUXEO_PLUGIN_TEMPLATES_DIR = "nx-plugin-templates";
	private static final String NUXEO_SCHEMA_TYPE_TEMPLATES_DIR = NUXEO_PLUGIN_TEMPLATES_DIR + "/" + "schema-type-templates";
	private static final String DOCTYPE_TEMPLATES_DIR = "doc-type-templates";
	private static final String SCHEMATYPE_TEMPLATES_DIR = "schema-type-templates";
	
	private static final String SERVICE_NAME_VAR = "${ServiceName}";
	private static final String SCHEMA_NAME_VAR = "${SchemaName}";
	private static final String REQUIRE_BUNDLE_LIST_VAR = "${Require-Bundle-List}";
	
	private static final String META_INF_DIR = "META-INF";
	private static final String MANIFEST_FILE = "MANIFEST.MF";
	
	private static final String OSGI_INF_DIR = "OSGI-INF";
	private static final String SCHEMAS_DIR = "schemas";
	private static final String JAR_EXT = ".jar";	

	private static final String TENANT_QUALIFIER = "Tenant";

	private static final String NX_PLUGIN_NAME = "nx_plugin_out";
	
	private HashMap<String, String> serviceSchemas = new HashMap<String, String>();
	private String tenantBindings = null;
	
	public HashMap<String, String> getServiceSchemas() {
		return serviceSchemas;
	}
	
	public XsdGeneration(String configfile, String recordType, String domain, String type, String schemaVersion) throws UIException {

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
				MakeXsd catlog = new MakeXsd(getTenantData(cspm));
				HashMap<String, String> definedSchemaList = catlog.getDefinedSchemas(
						record, recordType, schemaVersion);
				// For each schema defined in the configuration record, check to see if it was already
				// defined in another record.
				for (String definedSchema : definedSchemaList.keySet()) {
					if (serviceSchemas.containsKey(definedSchema) == false) {
						// If the newly defined schema doesn't already exist in our master list then add it
						try {
							createSchemaBundle(record, definedSchema, definedSchemaList);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						serviceSchemas.put(definedSchema, definedSchemaList.get(definedSchema));
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
			} else if (type.equals("delta")) { // Create the service bindings.
				Services tenantbob = new Services(getSpec(cspm), getTenantData(cspm),false);
				tenantBindings = tenantbob.doit();
			}
		} else {
			log.error(String.format("Record type '%s' is unknown in configuration file", configfile));
		}
	}
	
	private void createDocumentTypeBundle(Record record, HashMap<String, String> definedSchemaList) {
		// Create a new zip/jar for the doc type bundle
		// Create the META-INF folder and manifest file
		// Create the OSGI-INF folder and process the template files
		String docTypeName = record.getServicesTenantSg();
		if (record.isServicesExtension() == true) {
			docTypeName = docTypeName + TENANT_QUALIFIER + record.getSpec().getTenantID();
		}
		log.debug(String.format("Creating Nuxeo document type bundle: ${NuxeoDocTypeName}='%s'",
				docTypeName));
	}

	private void createSchemaBundle(Record record, String schemaName, HashMap<String, String>schemaList) throws Exception {
		String serviceName = record.getServicesTenantSg();
		String tenantName = record.getSpec().getAdminData().getTenantName();
		String schemaNameNoFileExt = schemaName.split("\\.")[0]; // Assumes a single '.' in a file name like "foo.xsd"
		String bundleName = tenantName + "." + serviceName + "." + schemaNameNoFileExt + JAR_EXT;
		
		//
		// Before creating a new bundle, make sure we haven't already created a bundle for this schema extension.
		//
		if (this.getServiceSchemas().containsKey(schemaName) == false) {
			File schemaTypeTemplatesDir = new File(NUXEO_SCHEMA_TYPE_TEMPLATES_DIR);
			if (schemaTypeTemplatesDir.exists()) {
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
						bundleName));
				
				// Check that the manifest template file exists
				File metaInfTemplate = new File(NUXEO_SCHEMA_TYPE_TEMPLATES_DIR + "/" + META_INF_DIR + "/" + MANIFEST_FILE);
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
				
				// Process the template by performing the substitutions
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				substitutionMap.put(SERVICE_NAME_VAR, serviceName);
				substitutionMap.put(SCHEMA_NAME_VAR, schemaNameNoFileExt);
				processTemplateFile(metaInfTemplate, substitutionMap, zos);
				
				zos.closeEntry();
				//
				// We're finished adding entries so close the zip output stream
				// 
				zos.close();
				
			} else {
				log.error(String.format("The '%s' directory is missing looking for it at '%s'.", NUXEO_SCHEMA_TYPE_TEMPLATES_DIR, schemaTypeTemplatesDir.getAbsolutePath()));
			}
		} else {
			log.warn(String.format("Nuxeo schema '%s' is being redefined in record '%s'.  Ignoring redefinition and *not* creating a new extension point bundle for the schema.",
					schemaName, record.getID()));
		}
	}

	private static String processTemplateFile(File templateFile, HashMap<String, String> substitutionMap, ZipOutputStream zos) throws Exception {
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
		if (bufferExceeded == 1) {
			String contentString = new String(buf, 0, fileSize);
			for (String key : substitutionMap.keySet()) {
				contentString = contentString.replace(key, substitutionMap.get(key));
				System.out.println(String.format("Replacing the string '%s' with '%s'", key, substitutionMap.get(key)));
			}
			// write the processed file out to the zip entry
			zos.write(contentString.getBytes(), 0, contentString.getBytes().length);
			result = contentString;
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
