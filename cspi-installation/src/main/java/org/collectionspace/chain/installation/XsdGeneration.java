package org.collectionspace.chain.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
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
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.csp.helper.test.TestConfigFinder;

import org.collectionspace.services.client.AbstractServiceClientImpl;
import org.collectionspace.services.common.api.CommonAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


public class XsdGeneration {
	private static final Logger log = LoggerFactory.getLogger(XsdGeneration.class);
		
	private static final String EOL = "\r\n"; // <CR><LF>
	private static final int MAX_CONFIG_FILE_SIZE = 100 * 1024;
	
	private static final boolean kIS_OSGI_MANIFEST = true;
	private static final boolean kNOT_AN_OSGI_MANIFEST = false;
		
	private static final String NUXEO_PLUGIN_TEMPLATES_DIR = "nx-plugin-templates";
	private static final String NUXEO_AUTH_TEMPLATES_PREFIX = "auth-";
	private static final String NUXEO_SCHEMA_TYPE_TEMPLATES_DIR = NUXEO_PLUGIN_TEMPLATES_DIR + "/" + "schema-type-templates";
	private static final String NUXEO_DOCTYPE_TEMPLATES_DIR = NUXEO_PLUGIN_TEMPLATES_DIR + "/" + "doc-type-templates";
	private static final String NUXEO_PARENT_AUTHORITY_TEMPLATE = "authorities_common.xsd";
	
	private static final String NUXEO_FOLDER_SUBTYPES_VAR = "${FolderSubtypes}";
	private static final String NUXEO_WORKSPACE_SUBTYPES_VAR = "${WorkspaceSubtypes}";
	
	private static final String DEFAULT_PARENT_TYPE = "CollectionSpaceDocument";
	private static final String SCHEMA_PARENT_TYPE_VAR = "${SchemaParentType}";
	private static final String COLLECTIONSPACE_CORE_SCHEMA_NAME_VAR = "${collectionspace_core}";	
	private static final String COLLECTIONSPACE_CORE_SCHEMA_NAME = "collectionspace_core";
	private static final String DEFAULT_BUNDLE_PREAMBLE = "collectionspace";
	private static final String DEFAULT_SYM_BUNDLE_PREAMBLE = "org" + "." + DEFAULT_BUNDLE_PREAMBLE;
	private static final String DEFAULT_AUTH_SYM_BUNDLE_PREAMBLE = "org" + "." + DEFAULT_BUNDLE_PREAMBLE + "." + "authority";
	private static final String SCHEMA_BUNDLE_QUALIFIER = "schema";
	private static final String DOCTYPE_BUNDLE_QUALIFIER = "doctype";
	private static final String SHARED_QUALIFIER = "shared";
		
	private static final String SCHEMA_NAME_VAR = "${SchemaName}";
	private static final String SCHEMA_NAME_LOWERCASE_VAR = "${SchemaName_LowerCase}";
	private static final String AUTHORITY_SCHEMA_NAME_VAR = "${AuthoritySchemaName}";	
//	private static final String SERVICE_AUTHORITY_NAME_SINGULAR_VAR = "${AuthorityNameSingular}";
	private static final String SERVICE_AUTHORITYITEM_NAME_SINGULAR_VAR = "${AuthorityItemNameSingular}";
	
	private static final String DOCTYPE_NAME_VAR = "${NuxeoDocTypeName}";
	private static final String DOCTYPE_NAME_LOWERCASE_VAR = "${NuxeoDocTypeName_LowerCase}";
	private static final String AUTHORITY_DOCTYPE_NAME_VAR = "${AuthorityNuxeoDocTypeName}";
	
	private static final String SERVICE_NAME_VAR = "${ServiceName}";
	private static final String SERVICE_NAME_LOWERCASE_VAR = "${ServiceName_LowerCase}";	
	
	private static final String DOCTYPE_DEFAULT_LIFECYCLE = "cs_default";
        private static final String DOCTYPE_LOCKING_LIFECYCLE = "cs_locking";
        private static final String DOCTYPE_LIFECYCLE_VAR = "${Lifecycle}";
	
	private static final String BUNDLE_SYM_NAME = "${BundleSymbolicName}";
	private static final String AUTH_BUNDLE_SYM_NAME = "${AuthBundleSymbolicName}";
	
	private static final String SUBTYPE_INDENT = "\t\t\t\t";  // 4 tab characters for indent of folder/workspace <Subtype> element indentation
	private static final String PREFETCH_INDENT = "\t\t\t\t";  // 4 tab characters for indent of <Prefetch> content lines
	
	private static final String REQUIRE_BUNDLE_LIST_VAR = "${Require-Bundle-List}";
	private static final String SCHEMA_ELEMENTS_LIST_VAR = "${SchemaElements}";
	private static final String PREFETCH_ELEMENT_LIST_VAR = "${PrefetchElement}";
	private static final String EMC_LAYOUT_LIST_VAR = "${LayoutList}";

	private static final String SCHEMA_ELEMENT_TEMPLATE = "<schema name=\"" + SCHEMA_NAME_VAR + "\" />";	
	private static final String META_INF_DIR = "META-INF";
	private static final String MANIFEST_FILE = "MANIFEST.MF";
	private static final String OSGI_INF_DIR = "OSGI-INF";
	private static final String SCHEMAS_DIR = "schemas";
	private static final String JAR_EXT = ".jar";	

	private static final String TENANT_QUALIFIER = "Tenant";
	private static final String NX_PLUGIN_NAME = "nx_plugin_out";
	private static final int MAX_OSGI_LINE_LEN = 72;
	
	private HashMap<String, String> serviceSchemas = new HashMap<String, String>();
	private HashMap<String, String> serviceSchemaBundles = new HashMap<String, String>();
	private HashMap<String, String> serviceDoctypeBundles = new HashMap<String, String>();
	
	private String tenantBindings = null;
	private File configBase = null;
	private Spec spec;

	public Spec getSpec() {
		return this.spec;
	}
	
	private void setSpec(Spec spec) {
		this.spec = spec;
	}
	
	public String getTenantBindings() {
		return this.tenantBindings;
	}
	
	public HashMap<String, String> getServiceSchemas() {
		return serviceSchemas;
	}
	
	public HashMap<String, String> getServiceSchemaBundles() {
		return serviceSchemaBundles;
	}
	
	public HashMap<String, String> getServiceDoctypeBundles() {
		return serviceDoctypeBundles;
	}
	
	private void dumpRecordServiceInfo(Record record) {
		PrintStream out = System.out;

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
			String[] recordPathList = record.getServicesRecordPathKeys();
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
			out.println(String.format("getServicesValidatorHandler = %s", record.getServicesValidatorHandler(false))); // 'false' means that we're not treating the record as an authority
			out.println(String.format("getServicesCommonList = %s", record.getServicesCommonList()));
			out.println(String.format("getServicesSchemaBaseLocation = %s", record.getServicesSchemaBaseLocation()));
			out.println(String.format("getServicesDocHandler = %s", record.getServicesDocHandler(record.isAuthorityItemType() == false)));
			out.println(String.format("getServicesListPath = %s", record.getServicesListPath()));
			out.println(String.format("getServicesFieldsPath = %s", record.getServicesFieldsPath()));
			out.println(String.format("getServicesSearchKeyword = %s", record.getServicesSearchKeyword()));
			
			out.println("##################################################");
		}
	}
	
	private boolean shouldGenerateBundles(Record record) {
		boolean result = true;
		
		if (record.isGenerateServicesSchema() == false) {
			result = false;
			log.debug(String.format("The application config record '%s' is already configured in the Services layer and will be skipped.",
					record.getID()));
		}
		
		String cmsType = record.getServicesCmsType();
		if (cmsType.equalsIgnoreCase("default") == false) {
			result = false;
			log.debug(String.format("The application config record '%s' declared an unkown cms-type '%s' for the Services layer and will be skipped.",
					record.getID(), cmsType));			
		}
		
		return result;
	}
	
	/*
	 * Depending up the 'generationType' passed in, this method creates either the Service bindings or the Service's Nuxeo doctype and
	 * Nuxeo schema bundles
	 */
	public XsdGeneration(
			File configfile, 
			String generationType, 
			String schemaVersion, 
			File bundlesOutputDir, 
			String serviceBindingsVersion) throws Exception {		
		CSPManager cspm=getServiceManager(configfile);
		Spec spec = createSpec(cspm);
		setSpec(spec);

		// 1. Setup a hashmap to keep track of which records and bundles we've already processed.
		// 2. Loop through all the known record types
		if (generationType.equals(CommonAPI.GENERATE_BUNDLES)) { // if 'true' then generate the schema and doctype bundles
			boolean docTypesCreated = true;
			for (Record record : spec.getAllRecords()) {
				if (log.isDebugEnabled() == true) {
					dumpRecordServiceInfo(record);
				}
				if (shouldGenerateBundles(record) == true) {
					MakeXsd catlog = new MakeXsd(getTenantData(cspm));
					HashMap<String, String> definedSchemaList = catlog.getDefinedSchemas(
							record, schemaVersion);
					// For each schema defined in the configuration record, check to see if it was already
					// defined in another record.
					boolean schemasCreated = true;
					for (String definedSchema : definedSchemaList.keySet()) {
						if (getServiceSchemas().containsKey(definedSchema) == false) {
							// If the newly defined schema doesn't already exist in our master list then add it
							try {
								createSchemaBundle(record, definedSchema, definedSchemaList, bundlesOutputDir);
								getServiceSchemas().put(definedSchema, definedSchemaList.get(definedSchema)); // Store a copy of the schema that was generated
								log.debug(String.format("New Services schema '%s' defined in Application configuration record '%s'.",
										definedSchema, record.getID()));
							} catch (Exception e) {
								schemasCreated = false;
								// TODO Auto-generated catch block
								log.error(String.format("Could not create schema bundle for '%s'.", definedSchema), e);
							}
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
					if (schemasCreated == true) {
						createDocumentTypeBundle(record, definedSchemaList, bundlesOutputDir);
					} else {
						docTypesCreated = false;
						log.error(String.format("Failed to create all the required schema bundles for App record ID='%s'.", record.getID()));
					}
				}
			}
			
			if (docTypesCreated == false) {
				String errMsg = String.format("Not all required document and schema bundles were created for the App config file '%s'.  See the log file for details.",
						configfile.getAbsolutePath());
				throw new Exception(errMsg);
			}
		} else if (generationType.equals(CommonAPI.GENERATE_BINDINGS)) { // Create the service bindings.
			Services tenantbob = new Services(createSpec(cspm), getTenantData(cspm),false);
			tenantBindings = tenantbob.doit(serviceBindingsVersion);
		} else {
			throw new Exception("Unknown generation type requested.");
		}
	}
	
	private String getRequiredBundlesList(Record record, HashMap<String, String> definedSchemaList) throws Exception {
		String result = null;
		StringBuffer strBuf = new StringBuffer();
		final String lineSeparator = "," + EOL + " "; // Need to start a new line with a <space> char
		
		for (String schemaName : definedSchemaList.keySet()) {
			String bundleName = getServiceSchemaBundles().get(schemaName);
			if (bundleName != null && bundleName.isEmpty() == false) {
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
	
	private void createDocumentTypeBundle(Record record,
			HashMap<String, String> definedSchemaList,
			File outputDir) throws Exception {
		boolean isAuthorityItemType = record.isAuthorityItemType();
                boolean supportsLocking = record.supportsLocking();
                String serviceName = record.getServicesTenantSg();
		String tenantName = record.getSpec().getAdminData().getTenantName();
		String docTypeName = record.getServicesTenantDoctype(false); // 'false' means we're not treating the record as an authority
		//
		// Compute what the doctype name should be based on tenancy and doctype extensions
		//
		String tenantQualifier = SHARED_QUALIFIER;
		if (record.isServicesExtension() == true) {
			tenantQualifier = tenantName;
			docTypeName = docTypeName + TENANT_QUALIFIER + record.getSpec().getTenantID();
		}
		String bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + serviceName.toLowerCase()
			+ "." + DOCTYPE_BUNDLE_QUALIFIER + "." + tenantQualifier + "." + docTypeName + JAR_EXT;
		bundleName = outputDir.getAbsolutePath() + "/" + bundleName;
		
		String templateDirPrefix = "";
		if (isAuthorityItemType == true) {
			templateDirPrefix = NUXEO_AUTH_TEMPLATES_PREFIX; // use the Authority based templates
		}		
		if (getServiceDoctypeBundles().containsKey(docTypeName) == false) {
			File doctypeTemplatesDir = new File(this.getConfigBase() + "/" + templateDirPrefix + NUXEO_DOCTYPE_TEMPLATES_DIR);
			if (doctypeTemplatesDir.exists() == true) {
				log.debug(String.format("### Creating Nuxeo document type ${NuxeoDocTypeName}='%s' in bundle: '%s'",
						docTypeName, bundleName));
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(bundleName));
				//
				// Create the manifest file from the doctype template
				//
				File metaInfTemplate = new File(doctypeTemplatesDir + "/" + META_INF_DIR + "/" + MANIFEST_FILE);				
				// Setup the hash map for the variable substitutions
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				substitutionMap.put(SERVICE_NAME_VAR, serviceName);
				substitutionMap.put(SERVICE_NAME_LOWERCASE_VAR, serviceName.toLowerCase());
                                if (supportsLocking) {
                                    substitutionMap.put(DOCTYPE_LIFECYCLE_VAR, DOCTYPE_LOCKING_LIFECYCLE);
                                } else {
                                    substitutionMap.put(DOCTYPE_LIFECYCLE_VAR, DOCTYPE_DEFAULT_LIFECYCLE);
                                }
                                substitutionMap.put(DOCTYPE_NAME_VAR, docTypeName);
				substitutionMap.put(DOCTYPE_NAME_LOWERCASE_VAR, docTypeName.toLowerCase());
				if (isAuthorityItemType == true) {
					String authoritySchemaName = FilenameUtils.removeExtension(getAuthoritiesCommonName(record));
					substitutionMap.put(AUTHORITY_SCHEMA_NAME_VAR, authoritySchemaName.toLowerCase());		
					substitutionMap.put(AUTHORITY_DOCTYPE_NAME_VAR, record.getServicesTenantAuthSg());
					substitutionMap.put(COLLECTIONSPACE_CORE_SCHEMA_NAME_VAR, COLLECTIONSPACE_CORE_SCHEMA_NAME);
//					substitutionMap.put(SERVICE_AUTHORITY_NAME_SINGULAR_VAR, record.getServicesTenantAuthSg().toLowerCase());
//					substitutionMap.put(SERVICE_AUTHORITYITEM_NAME_SINGULAR_VAR, record.getServicesTenantSg().toLowerCase());	
				}
				substitutionMap.put(EMC_LAYOUT_LIST_VAR, ""); // Once we re-enable the Nuxeo DM webapp, this should be list of layout definitions
				String requiredBundleList = getRequiredBundlesList(record, definedSchemaList);
				substitutionMap.put(REQUIRE_BUNDLE_LIST_VAR, requiredBundleList);
				createManifestFile(metaInfTemplate, substitutionMap, zos);
				//
				// Next, create the OSGI-INF files
				//
				String schemaElementsList = getSchemaElementsList(record, definedSchemaList);
				substitutionMap.put(SCHEMA_ELEMENTS_LIST_VAR, schemaElementsList);
				substitutionMap.put(PREFETCH_ELEMENT_LIST_VAR, getPrefetchElement(record));
				
				String schemaParentType = DEFAULT_PARENT_TYPE;
				if (record.isServicesExtension() == true) { // If we know this is an extension of a common base type, we need to set the parent type
                                    // CSPACE-6227: When extending base doctypes, get the actual parent doctype.
                                    // At present, the code line below will not insert the correct 'extends="..."' value in
                                    // the (as-yet) non-standard case of extending an authority doctype (e.g. Personauthority),
                                    // as contrasted with extending an authority item doctype (e.g. Person). - ADR 2013-09-26
                                    schemaParentType = record.getServicesTenantDoctype(false);
				}
				substitutionMap.put(SCHEMA_PARENT_TYPE_VAR, schemaParentType);
				
				String folderSubtypes = getFolderSubtypes(record);
				substitutionMap.put(NUXEO_FOLDER_SUBTYPES_VAR, folderSubtypes);
				
				String workspaceSubtypes = getWorkspaceSubtypes(record);
				substitutionMap.put(NUXEO_WORKSPACE_SUBTYPES_VAR, workspaceSubtypes);
								
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
			String errMsg = String.format("Nuxeo document type '%s' already exists.  Skipping creation.", docTypeName);
			throw new Exception(errMsg);
		}
	}

	/*
	 * Return the Nuxeo <prefetch> element for the Nuxeo doctype.
	 */
	private String getPrefetchElement(Record record) {
		String result = "";
		
		Set<String> prefetchFieldList = record.getServicesPrefetchFields();
		if (prefetchFieldList != null && prefetchFieldList.isEmpty() == false) {
			StringBuffer out = new StringBuffer();
			out.append("<prefetch>\n");
			for (String prefetchField : prefetchFieldList) {
				out.append(PREFETCH_INDENT);
				out.append(prefetchField);
				out.append('\n');
			}
			out.append(PREFETCH_INDENT);
			out = out.deleteCharAt(out.length() - 1); // delete the last '\t' character to align with opening <prefetch> element
			out.append("</prefetch>");
			result = out.toString();
		}
		
		return result;
	}

	/*
	 * Return the Nuxeo EMC "Folder" <Subtype> declarations
	 */
	private String getFolderSubtypes(Record record) {
		Set<String> folderSubtypeList = record.getServicesFolderSubtypes();
		return getSubtypes(folderSubtypeList);
	}
	
	/*
	 * Return the Nuxeo EMC "Workspace" <Subtype> declarations
	 */	
	private String getWorkspaceSubtypes(Record record) {
		Set<String> workspaceSubtypeList = record.getServicesWorkspaceSubtypes();
		return getSubtypes(workspaceSubtypeList);
	}	
	
	/*
	 * Given a set of strings, we create something like the following
     * <subtypes>
     *   <type>Blob</type>
     *   <type>Foo</type>
     * </subtypes>
	 * 
	 */
	private String getSubtypes(Set<String> subtypeList) {
		String result = ""; //empty string
		
		if (subtypeList != null && subtypeList.isEmpty() == false) {
			StringBuffer elementList = new StringBuffer();
			for (String folderSubtype : subtypeList) {
				//
				// Creates a <type>theTypeName</type> element
				//
				elementList.append("<type>");
				elementList.append(folderSubtype);
				elementList.append("</type>");
				elementList.append('\n');
				elementList.append(SUBTYPE_INDENT);
			}
			result = elementList.substring(0, elementList.length() - SUBTYPE_INDENT.length() - 1); // Remove the last end-of-line and tabs
		}
		
		return result;
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
				processTemplateFile(osgiFile, substitutionMap, zos, kNOT_AN_OSGI_MANIFEST);	
				zos.closeEntry();
			} else {
				log.error(String.format("Ignoring directory '%s' while processing OSGI-INF files.", osgiFile.getAbsolutePath()));
			}
		}
	}
	
	private String getAuthoritiesCommonName(Record record) {
		String result = null;
		
		try {
			String authoritieCommonName = record.getServicesTenantAuthPl() + AbstractServiceClientImpl.PART_LABEL_SEPARATOR +
					AbstractServiceClientImpl.PART_COMMON_LABEL + ".xsd" ;
			result = authoritieCommonName;
		} catch (NullPointerException e) {
			log.error(e.getMessage(), e);
		}
		
		return result;
	}
	
	private String getAuthorityItemsCommonName(Record record) {
		String result = null;
		
		try {
			String authoritieCommonName = record.getServicesTenantPl() + AbstractServiceClientImpl.PART_LABEL_SEPARATOR +
					AbstractServiceClientImpl.PART_COMMON_LABEL + ".xsd" ;
			result = authoritieCommonName.toLowerCase();
		} catch (NullPointerException e) {
			log.error(e.getMessage(), e);
		}
		
		return result;
	}
	
	
	/*
	 * This method uses a template file to create the parent authority schema for the record type
	 * that is being passed in.  All CollectionSpace authorities use the same set of fields in their schema.
	 */
	private void createParentAuthorityEntry(Record record, 
			File schemaTypeTemplatesDir,
			HashMap<String, String> substitutionMap,
			ZipOutputStream zos) throws Exception {
		//
		// Ensure that the parent authority template exists in the "schemas' directory
		//
		File parentAuthorityTemplate = new File(schemaTypeTemplatesDir.getAbsolutePath() + "/" + SCHEMAS_DIR + "/" +
				NUXEO_PARENT_AUTHORITY_TEMPLATE);
		if (parentAuthorityTemplate.exists() == false) {
			throw new Exception(String.format("The %s template '%s' is missing.", NUXEO_PARENT_AUTHORITY_TEMPLATE, 
					parentAuthorityTemplate.getAbsolutePath()));
		}
		
		String authoritieCommonName = getAuthoritiesCommonName(record).toLowerCase();
		zos.putNextEntry(new ZipEntry(SCHEMAS_DIR + "/" + authoritieCommonName));
		processTemplateFile(parentAuthorityTemplate, substitutionMap, zos, kNOT_AN_OSGI_MANIFEST);
	}
	
	/*
	 * Creates an entry in the zip/jar bundle for XML Schema (.xsd) files.
	 */
	private void createSchemaFiles(Record record,
			File schemaTypeTemplatesDir,
			HashMap<String, String> substitutionMap,
			ZipOutputStream zos,
			String schemaName,
			HashMap<String, String>definedSchemaList) throws Exception {
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
			//
			// If the record type is an authority item then we need to create the corresponding parent authority type
			//
			if (shouldCreateAuthoritySchema(record, schemaName) == true) {
				createParentAuthorityEntry(record, schemaTypeTemplatesDir, substitutionMap, zos);
			}
			zos.closeEntry(); // flush and close the "schemas" entry in the zip/jar file
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
		processTemplateFile(metaInfTemplate, substitutionMap, zos, kIS_OSGI_MANIFEST);				
		zos.closeEntry();		
	}
	
	boolean shouldCreateAuthoritySchema(Record record, String schemaName) {
		boolean result = false;
		
		//
		// Return true if the record is an authority item and the schema name passed in matches the record's primary name.
		// Use this method to determine if we need to create an Authority schema
		//
		String authItemsCommonName = getAuthorityItemsCommonName(record);
		if (record.isAuthorityItemType() && schemaName.equalsIgnoreCase(authItemsCommonName)) {
			result = true;
		}
		
		return result;
	}
	
	private void createSchemaBundle(Record record,
			String schemaName,
			HashMap<String, String>definedSchemaList,
			File outputDir) throws Exception {
		String serviceName = record.getServicesTenantSg();
		String schemaNameNoFileExt = FilenameUtils.removeExtension(schemaName);
		String tenantName = record.getSpec().getAdminData().getTenantName();
		//
		// Figure out what the bundle name should be.
		//
		String bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + serviceName.toLowerCase()
				+ "." + SCHEMA_BUNDLE_QUALIFIER + "." + schemaNameNoFileExt;
		if (isGlobalSchema(schemaNameNoFileExt) == true) {
			bundleName = DEFAULT_BUNDLE_PREAMBLE + "." + SHARED_QUALIFIER + "." + SCHEMA_BUNDLE_QUALIFIER + "." + schemaNameNoFileExt;
		}
		bundleName = outputDir.getAbsolutePath() + "/" + bundleName + JAR_EXT;
		
		//
		// Before creating a new bundle, make sure we haven't already created a bundle for this schema extension.
		//
		if (getServiceSchemaBundles().containsKey(schemaName) == false) {
			//
			// Find the correct templates directory to use
			//
			String templateDirPrefix = "";
			if (shouldCreateAuthoritySchema(record, schemaName) == true) {
				templateDirPrefix = NUXEO_AUTH_TEMPLATES_PREFIX; // use the Authority based templates variant
			}
			File schemaTypeTemplatesDir = new File(this.getConfigBase() + "/" + templateDirPrefix + NUXEO_SCHEMA_TYPE_TEMPLATES_DIR);
			
			if (schemaTypeTemplatesDir.exists() == true) {
				File outputFile = new File(bundleName);
				if (log.isDebugEnabled() == true) {
					log.debug(String.format("Creating new jar file: '%s'", outputFile.getAbsolutePath()));
				}
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
				//
				// Create the manifest file from the schema type template
				//
				File metaInfTemplate = new File(schemaTypeTemplatesDir + "/" + META_INF_DIR + "/" + MANIFEST_FILE);
				// Setup the hash map for the variable substitutions
				String serviceNameVar = serviceName;				
				if (isGlobalSchema(schemaNameNoFileExt) == true) {
					serviceNameVar = SCHEMA_BUNDLE_QUALIFIER;
				}
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				// Symbolic bundle name
				// org.collectionspace.${ServiceName_LowerCase}.${SchemaName_LowerCase}
				String bundleSymbolicName = DEFAULT_SYM_BUNDLE_PREAMBLE + "." + serviceNameVar.toLowerCase()  + "." +
						schemaNameNoFileExt.toLowerCase();
				substitutionMap.put(BUNDLE_SYM_NAME, bundleSymbolicName);
				substitutionMap.put(SERVICE_NAME_VAR, serviceNameVar);
				substitutionMap.put(SERVICE_NAME_LOWERCASE_VAR, serviceNameVar.toLowerCase());
				substitutionMap.put(SCHEMA_NAME_VAR, schemaNameNoFileExt);
				substitutionMap.put(SCHEMA_NAME_LOWERCASE_VAR, schemaNameNoFileExt.toLowerCase());
				if (shouldCreateAuthoritySchema(record, schemaName) == true) {
					String authoritySchemaName = FilenameUtils.removeExtension(getAuthoritiesCommonName(record));
					substitutionMap.put(AUTHORITY_SCHEMA_NAME_VAR, authoritySchemaName.toLowerCase());		
					substitutionMap.put(SERVICE_AUTHORITYITEM_NAME_SINGULAR_VAR, record.getServicesTenantSg().toLowerCase());
					bundleSymbolicName = DEFAULT_AUTH_SYM_BUNDLE_PREAMBLE + "." + serviceNameVar.toLowerCase();					
					substitutionMap.put(AUTH_BUNDLE_SYM_NAME, bundleSymbolicName);
				}
				//
				// Create the manifest file
				//
				createManifestFile(metaInfTemplate, substitutionMap, zos);
				//
				// Next, crate the OSGI-INF files
				//
				createOsgiInfFiles(schemaTypeTemplatesDir, substitutionMap, zos);
				//
				// Finally, create the "schemas" directory entry and add the schema file to it
				//
				createSchemaFiles(record, schemaTypeTemplatesDir, substitutionMap, zos, schemaName, definedSchemaList);
				//
				// We're finished adding entries so close the zip output stream
				//
				zos.close();
				//
				// Keep track of the schema bundles we've created
				//
				getServiceSchemaBundles().put(schemaName, bundleSymbolicName); // keep track of the bundles we've created
			} else {
				String errMsg = String.format("The '%s' directory is missing looging for it at '%s'.", NUXEO_SCHEMA_TYPE_TEMPLATES_DIR,
						schemaTypeTemplatesDir.getAbsolutePath());
				throw new Exception(errMsg);
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
			ZipOutputStream zos,
			boolean isOSGIManifest) throws Exception {
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
				log.debug(String.format("Replacing the string '%s' with '%s'", key, varReplacementStr));
			}
			// write the processed file out to the zip entry if they passed one in
			result = isOSGIManifest ? osgiManifestFormat(contentStr) : contentStr;
			if (zos != null) {
				zos.write(result.getBytes(), 0, result.getBytes().length);
			}
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
	
	private static boolean isEOLChar(char c) {
		boolean result = false;
		
		if (c == '\n' || c == '\r') {
			result = true;
		}
		
		return result;
	}
	
	/*
	 * OSGI manifest files cannot have lines that are more than 71 characters.  Also, continuation lines must begin with a <space> character
	 */
	private static String osgiManifestFormat(String srcStr) {
		StringBuffer srcBuf = new StringBuffer(srcStr);
		StringBuffer destBuf = new StringBuffer();
		String result = null;
		
		int total = srcStr.length();
		int currentLineLen = 1;
		int offset = 0;
		for (int i = 0; i < total; i++) {
			char currentChar = srcBuf.charAt(i);
			if (currentChar == '\n') {
				currentLineLen = 1;
			}
			if (currentLineLen++ % MAX_OSGI_LINE_LEN == 0 && isEOLChar(currentChar) == false) {
				destBuf.insert(offset, EOL + " ");
				offset += EOL.length() + 1;
				currentLineLen = 1;
			}
			destBuf.insert(offset++, currentChar);
		}
		
		result = destBuf.toString();
		return result;
	}

	/*
	private InputSource getSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream(fallbackFile, isFromBuild());
		} catch (CSPDependencyException e) {
			String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
			return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
		}
	}
	*/
	
	private InputSource getSource(File file) {
		InputSource result = null;
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			result = new InputSource(inputStream);
		} catch (FileNotFoundException e) {
			log.error(String.format("Could not create an InputSource instance from file '%s'.", file.getAbsolutePath()), e);
		}
		
		return result;
	}	
	
	private File getSourceAsFile(String fallbackFile) {
		File result = null;

		try {
			result = TestConfigFinder.getConfigFile(fallbackFile);
		} catch (CSPDependencyException e) {
			String name = getClass().getPackage().getName()
					.replaceAll("\\.", "/")
					+ "/" + fallbackFile;
			URL fileURL = Thread.currentThread().getContextClassLoader()
					.getResource(name);
			if (fileURL != null) {
				String fileName = fileURL.getFile();
				if (fileName != null) {
					File file = new File(fileName);
					if (file.exists() == true) {
						result = file;
					} else {
						log.error(String.format("Could not find App config file '%s' from URL '%s'", fileName, fileURL));
					}
				} else {
					log.error(String.format("Could not find App config file '%s' from URL '%s'", name, fileURL));
				}
			} else {
				log.error(String.format("Could not find App config file '%s'", name));
			}
		}

		return result;
	}
	
	private Spec createSpec(CSPManager cspm){
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		return spec;
	}

	private TenantSpec getTenantData(CSPManager cspm) {
		
		ServicesStorageGenerator gen=(ServicesStorageGenerator)cspm.getStorage("service");
		TenantSpec td = gen.getTenantData();
		return td;
	}
	
	private CSPManager getServiceManager(File configFile) {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		try {
			cspm.go();
			File configBase = configFile.getParentFile();
			this.setConfigBase(configBase);
			cspm.setConfigBase(configBase); // Saves a copy of the base config directory
			cspm.configure(getSource(configFile), new ConfigFinder(null, configBase));
		} catch (CSPDependencyException e) {
			log.error("CSPManagerImpl failed");
			log.error(e.getLocalizedMessage() );
		}
		return cspm;
		
	}

	private void setConfigBase(File configBase) {
		this.configBase = configBase;
	}
	
	private File getConfigBase() {
		return this.configBase;
	}
}
