package org.collectionspace.chain.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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


public class ServiceConfigGeneration {
	private static final Logger log = LoggerFactory.getLogger(ServiceConfigGeneration.class);
		
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
    private static final String DOCTYPE_LOCKING_LIFECYCLE = "cs_locking"; // Used for Movement records
    private static final String DOCTYPE_REPLICATING_LIFECYCLE = "cs_replicating"; // Used for Shared Authority records
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
	
	// generated config (schemas and complex types) for other tenants.  We use this to check for redefinitions of schemas and complex types.
	// The map's key is the top level tenant config file name -e.g., "botgarden-tenant.xml"
	private Map<String, ServiceConfigGeneration> tenantConfigMap;
	
	//
	private File infoOutputDir = null;
	
	// keeps track of schemas defined in each record
	private Map<Record, Map<String,String>> recordDefinedSchemasMap = new HashMap<Record, Map<String, String>>(); // Map<Record, Map<SchemaName, XSD>>
	
	// keeps track of XSD defined complex types defined in each record.
	private Map<Record, Map<String, Map<String, String>>> recordDefinedComplexTypesMap = new HashMap<Record, Map<String, Map<String, String>>>(); // Map<Record, Map<SchemaName, Map<TypeName, XSD>>>
	
	// keeps track of Schemas defined in a tenant
	private Map<String, Map<Record, Map<String, String>>> tenantSchemasMap = new HashMap<String, Map<Record, Map<String, String>>>(); // Map<TenantName, Map<Record, Map<SchemaName, XSD>>>
	
	private Map<String, String> serviceSchemaBundles = new HashMap<String, String>();
	private Map<String, String> serviceDoctypeBundles = new HashMap<String, String>();
	
	private String tenantBindings = null;
	private File configBase = null;
	private File configFile = null;
	private Spec spec;
	
	public Map<String, String> getDefinedSchemas() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		
		for (Record record : this.spec.getAllRecords()) {
			Map<String, String> schemaMap = getRecordDefinedSchemasMap().get(record); // get a map of all the schemas in defined in the record
			for (String schemaName : schemaMap.keySet()) {
				if (schemaMap.get(schemaName) == null) {
					result.put(schemaName, schemaMap.get(schemaName));
				} else {
					String msg = String.format("Schema named '%s' is defined more than once while processing config for '%s'.",
							schemaName, this.getConfigFile().getName());
					throw new Exception(msg);
				}
			}
		}
		
		return result;
	}
	
	public boolean isExistingSchema(Record currentRecord, String schemaName, String xsdDef) throws Exception {
		boolean result = false;
		
		for (Record record : this.spec.getAllRecords()) {
			Map<String, String> schemaMap = getRecordDefinedSchemasMap().get(record);
			for (String skemaName : schemaMap.keySet()) {
				String existingXsdDef = schemaMap.get(skemaName);
				if (existingXsdDef != null) {
					result = true;
					if (areXsdSnippetsEqual(existingXsdDef, xsdDef) == false) {
						String msg = String.format("Config Generation: '%s' - Services schema '%s' defined in record '%s' was previously defined differently in a record '%s' of tenant ID=%s.",
								configFile.getName(), schemaName, currentRecord.getID(), record.getID(), record.getSpec().getTenantID());
						throw new Exception(msg);
					} else {
						// Otherwise, it already exists so emit a warning just in case it shouldn't have been redefined.
						log.warn(String.format("Config Generation: '%s' - Services schema '%s' defined in record '%s', but was previously defined in record '%s' of tenant ID='%s'.",
								configFile.getName(), schemaName, currentRecord.getID(), record.getID(), record.getSpec().getTenantID()));
					}
				}
			}
		}
		
		return result;
	}

	public Spec getSpec() {
		return this.spec;
	}
	
	private void setSpec(Spec spec) {
		this.spec = spec;
	}
	
	public String getTenantBindings() {
		return this.tenantBindings;
	}
	
	public Map<String, ServiceConfigGeneration> getTenantConfigMap() {
		return this.tenantConfigMap;
	}
	
//	public Map<String, String> getExistingServiceSchemas() {
//		return serviceSchemas;
//	}
	
	public Map<String, String> getServiceSchemaBundles() {
		return serviceSchemaBundles;
	}
	
	public Map<String, String> getServiceDoctypeBundles() {
		return serviceDoctypeBundles;
	}
	
	/**
	 * Returns a map containing all the complex XSD types defined on a per record basis.
	 * @return
	 */	
	public Map<Record, Map<String, Map<String, String>>> getRecordDefinedComplexTypesMap() {
		return recordDefinedComplexTypesMap;
	}
	
	/**
	 * Returns a map containing all the schemas defined on a per record basis.
	 * @return
	 */
	public Map<Record, Map<String, String>> getRecordDefinedSchemasMap() {
		return recordDefinedSchemasMap;
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
	public ServiceConfigGeneration(
			Map<String, ServiceConfigGeneration> tenantConfigMap,
			File configfile, 
			String generationType, 
			String schemaVersion, 
			File bundlesOutputDir, 
			String serviceBindingsVersion,
			File infoOutputDir) throws Exception {
		this.infoOutputDir = infoOutputDir;
		this.tenantConfigMap = tenantConfigMap;		
		CSPManager cspm = getServiceManager(configfile);		
		Spec spec = createSpec(cspm);
		setSpec(spec);

		if (generationType.equals(CommonAPI.GENERATE_BUNDLES)) { // if 'true' then generate the schema and doctype bundles
			log.info(String.format("Config Generation: '%s' - ### Generating Service bundles from '%s'.", 
					configfile.getName(), configfile.getPath()));
			
			boolean docTypesCreated = true;
			for (Record record : spec.getAllRecords()) {  // For each record in the current spec (tenant config), create related Service bundle (schema and Nuxeo doctype declaration).
				record.setConfigFileName(configFile.getName());
				if (log.isDebugEnabled() == true) {
					dumpRecordServiceInfo(record);
				}
				
				if (shouldGenerateBundles(record) == true) {
					MakeXsd xsdGenerator = new MakeXsd(this, getTenantData(cspm));
					Map<String, String> newSchemasMap = xsdGenerator.generateSchemasForRecord(record, schemaVersion);
					
					if (newSchemasMap.isEmpty() == false) {
						getRecordDefinedSchemasMap().put(record, newSchemasMap);
						if (xsdGenerator.getSchemaDefinedComplexTypes().isEmpty() == false) {
							getRecordDefinedComplexTypesMap().put(record, xsdGenerator.getSchemaDefinedComplexTypes()); // We need to keep track of all Group type (aka, xsd complex type) definitions to prevent accidental duplicates
						}

						boolean schemasCreated = true;
						for (String newSchemaName : newSchemasMap.keySet()) {
								// If the newly defined schema doesn't already exist in our master list then add it
								try {
									createSchemaBundle(record, newSchemaName, newSchemasMap, bundlesOutputDir);
								} catch (Exception e) {
									schemasCreated = false;
									log.error(String.format("Config Generation: '%s' - Could not create schema bundle for schema '%s' defined in record '%s'.",
											configfile.getName(), newSchemaName, record.getID()), e);
								}
							}
						//
						// Create the Nuxeo bundle document type
						//
						if (schemasCreated == true) {
							createDocumentTypeBundle(record, newSchemasMap, bundlesOutputDir);
						} else {
							docTypesCreated = false;
							log.error(String.format("Config Generation: '%s' - Failed to create all the required schema bundles for App record ID='%s'.",
									configfile.getName(), record.getID()));
						}

					}
				}
			}
			
			if (docTypesCreated == false) {
				String errMsg = String.format("Config Generation: '%s' - Not all required document and schema bundles were created for the App config file '%s'.  See the log file for details.",
						configfile.getName(), configfile.getAbsolutePath());
				throw new Exception(errMsg);
			}
		} else if (generationType.equals(CommonAPI.GENERATE_BINDINGS)) { // Create the service bindings.
			log.info(String.format("Config Generation: '%s' - ### Generating Service bindings from '%s'.", 
					configfile.getName(), configfile.getPath()));
			ServiceBindingsGeneration tenantbob = new ServiceBindingsGeneration(getConfigFile(), createSpec(cspm), getTenantData(cspm), false, this.infoOutputDir);
			tenantBindings = tenantbob.doit(serviceBindingsVersion);
		} else {
			throw new Exception(String.format("Config Generation: '%s' - Unknown generation type '%s' requested.", 
					configfile.getName(), generationType));
		}
	}
		
	private String getRequiredBundlesList(Record record, Map<String, String> definedSchemaList) throws Exception {
		String result = null;
		StringBuffer strBuf = new StringBuffer();
		final String lineSeparator = "," + EOL + " "; // Need to start a new line with a <space> char
		
		for (String schemaName : definedSchemaList.keySet()) {
			String bundleName = getServiceSchemaBundles().get(schemaName);
			if (bundleName != null && bundleName.isEmpty() == false) {
				log.debug(String.format("Config Generation: '%s' - Included in Require-Bundle list entry: '%s'",
						this.getConfigFile().getName(), bundleName));
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
	
	private String getSchemaElementsList(Record record, Map<String, String> definedSchemaList) throws Exception {
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
			Map<String, String> definedSchemaList,
			File outputDir) throws Exception {
		boolean isAuthorityItemType = record.isAuthorityItemType();
                boolean supportsLocking = record.supportsLocking();
                boolean supportsReplicating = record.supportsReplicating();
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
		if (doesDoctypeBundleAlreadyExist(docTypeName) == false) {
			File doctypeTemplatesDir = new File(this.getConfigBase() + "/" + templateDirPrefix + NUXEO_DOCTYPE_TEMPLATES_DIR);
			if (doctypeTemplatesDir.exists() == true) {
				log.info(String.format("Config Generation: '%s' - New Services document type ${NuxeoDocTypeName}='%s' created in bundle: '%s'",
						this.getConfigFile().getName(), docTypeName, bundleName));
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(bundleName));
				//
				// Create the manifest file from the doctype template
				//
				File metaInfTemplate = new File(doctypeTemplatesDir + "/" + META_INF_DIR + "/" + MANIFEST_FILE);				
				// Setup the hash map for the variable substitutions
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				substitutionMap.put(SERVICE_NAME_VAR, serviceName);
				substitutionMap.put(SERVICE_NAME_LOWERCASE_VAR, serviceName.toLowerCase());
				// Set the document workflow lifecycle type
                if (supportsLocking) {
                    substitutionMap.put(DOCTYPE_LIFECYCLE_VAR, DOCTYPE_LOCKING_LIFECYCLE);
                } else if (supportsReplicating) {
                    substitutionMap.put(DOCTYPE_LIFECYCLE_VAR, DOCTYPE_REPLICATING_LIFECYCLE);
                } else {
                    substitutionMap.put(DOCTYPE_LIFECYCLE_VAR, DOCTYPE_DEFAULT_LIFECYCLE);
                }
                // Set the resource/document type names
                substitutionMap.put(DOCTYPE_NAME_VAR, docTypeName);
				substitutionMap.put(DOCTYPE_NAME_LOWERCASE_VAR, docTypeName.toLowerCase());
				// Authority items require additional names
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
			String errMsg = String.format("Config Generation: '%s' - Nuxeo document type '%s' already exists.  Skipping creation.",
					this.getConfigFile().getName(), docTypeName);
			log.trace(errMsg);
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
			throw new Exception(String.format("Config Generation: '%s' - The %s directory '%s' is missing.", OSGI_INF_DIR, 
					this.getConfigFile().getName(), osgiDir.getAbsolutePath()));
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
				log.error(String.format("Config Generation: '%s' - Ignoring directory '%s' while processing OSGI-INF files.", 
						this.getConfigFile().getName(), osgiFile.getAbsolutePath()));
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
			Map<String, String> substitutionMap,
			ZipOutputStream zos) throws Exception {
		//
		// Ensure that the parent authority template exists in the "schemas' directory
		//
		File parentAuthorityTemplate = new File(schemaTypeTemplatesDir.getAbsolutePath() + "/" + SCHEMAS_DIR + "/" +
				NUXEO_PARENT_AUTHORITY_TEMPLATE);
		if (parentAuthorityTemplate.exists() == false) {
			throw new Exception(String.format("Config Generation: '%s' - The %s template '%s' is missing.",
					this.getConfigFile().getName(), NUXEO_PARENT_AUTHORITY_TEMPLATE, parentAuthorityTemplate.getAbsolutePath()));
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
			Map<String, String> substitutionMap,
			ZipOutputStream zos,
			String schemaName,
			Map<String, String>definedSchemaList) throws Exception {
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
			String errMsg = String.format("Config Generation: '%s' - The contents for the schema '%s' is empty or missing.",
					this.getConfigFile().getName(), schemaName);
			log.error(errMsg);
			throw new Exception(errMsg);
		}
	}
	
	private void createManifestFile(File metaInfTemplate, Map<String, String> substitutionMap, ZipOutputStream zos) throws Exception {
		if (metaInfTemplate.exists() == false) {
			throw new Exception(String.format("Config Generation: '%s' - The manifest template file '%s' is missing:",
					this.getConfigFile().getName(), metaInfTemplate.getAbsolutePath()));
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
	
	private boolean doesSchemaBundleAlreadyExist(String schemaName) {
		boolean result = getServiceSchemaBundles().containsKey(schemaName); // Check to see if we're already created it in the current tenant's config
		
		if (result == false) { // Check to see if we've defined it in another tenant already
			for (String configFileName : tenantConfigMap.keySet()) {
				ServiceConfigGeneration tenantConfig = tenantConfigMap.get(configFileName);
				if (tenantConfig.getServiceSchemaBundles().get(schemaName) != null) {
					String msg = String.format("Config Generation: '%s' - Skipping schema bundle creation.  An identical schema bundle for '%s' was already created using the tenant config from '%s'.", 
							this.getConfigFile().getName(), schemaName, configFileName);
					log.trace(msg);
					return true;
				}
			}
		}
		
		return result;
	}
	
	private boolean doesDoctypeBundleAlreadyExist(String docTypeName) {
		boolean result = getServiceDoctypeBundles().containsKey(docTypeName);
		
		if (result == false) {
			for (String configFileName : tenantConfigMap.keySet()) {
				ServiceConfigGeneration tenantConfig = tenantConfigMap.get(configFileName);
				if (tenantConfig.getServiceDoctypeBundles().get(docTypeName) != null) {
					String msg = String.format("Config Generation: '%s' - Skipping doctype bundle creation.  An identical doctype bundle for '%s' was already created using the tenant config from '%s'.", 
							this.getConfigFile().getName(), docTypeName, configFileName);
					log.trace(msg);
					return true;
				}

			}
		}
		
		return result;
	}
	
	private void createSchemaBundle(
			Record record,
			String schemaName,
			Map<String, String>definedSchemaList,
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
		// Setup other related names
		String serviceNameVar = serviceName;				
		if (isGlobalSchema(schemaNameNoFileExt) == true) {
			serviceNameVar = SCHEMA_BUNDLE_QUALIFIER;
		}
		String bundleSymbolicName = DEFAULT_SYM_BUNDLE_PREAMBLE + "." + serviceNameVar.toLowerCase()  + "." +
				schemaNameNoFileExt.toLowerCase();

		//
		// Before creating a new bundle, make sure we haven't already created a bundle for this schema extension.
		//
		if (doesSchemaBundleAlreadyExist(schemaName) == false) {
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
				log.trace(String.format("Config Generation: '%s' - Creating new jar file: '%s'", 
						this.getConfigFile().getName(), outputFile.getAbsolutePath()));
				ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
				//
				// Create the manifest file from the schema type template
				//
				File metaInfTemplate = new File(schemaTypeTemplatesDir + "/" + META_INF_DIR + "/" + MANIFEST_FILE);
				// Setup the hash map for the variable substitutions
				HashMap<String, String> substitutionMap = new HashMap<String, String>();
				// Symbolic bundle name
				// org.collectionspace.${ServiceName_LowerCase}.${SchemaName_LowerCase}
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
				// Log the event
				//
				log.info(String.format("Config Generation: '%s' - New Services schema bundle for '%s' defined in Application configuration record '%s': '%s'",
						this.getConfigFile().getName(), schemaName, record.getID(), outputFile.getPath()));
			} else {
				String errMsg = String.format("Config Generation: '%s' - The '%s' directory is missing at '%s'.", 
						this.getConfigFile().getName(), NUXEO_SCHEMA_TYPE_TEMPLATES_DIR, schemaTypeTemplatesDir.getAbsolutePath());
				throw new Exception(errMsg);
			}
		} else {
			log.trace(String.format("Config Generation: '%s' - Skipping schema bundle creation. A Nuxeo schema bundle for the schema '%s' declared in '%s' already exists.",
					this.getConfigFile().getName(), schemaName, record.getID()));
		}

		//
		// Keep track of the schema bundles we've processed
		//
		getServiceSchemaBundles().put(schemaName, bundleSymbolicName); // keep track of the bundles we've created
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
			Map<String, String> substitutionMap,
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
			String errMsg = String.format("Config Generation - The file '%s' was empty or missing.",
					templateFile.getAbsoluteFile());
			if (bufferExceeded > 1) {
				errMsg = String.format("Config Generation - The file '%s' was too large to fit in our memory buffer.  It needs to be less than %d bytes, but was at least %d bytes in size.",
						templateFile.getAbsoluteFile(), MAX_CONFIG_FILE_SIZE, fileSize);
			}
			throw new Exception(errMsg);
		}
		
		log.debug(String.format("Config Generation - The processed file is:\n%s", result));
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
			result.setPublicId(file.getName());
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
	
	private CSPManager getServiceManager(File configFile) throws Exception {
		CSPManager result = null;
		
		CSPManager cspm = new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.register(new ServicesStorageGenerator());
		cspm.go(); // Do more initialization of our CSPManagerImpl instance (i.e., cspm)
		File configBase = configFile.getParentFile();
		cspm.setConfigBase(configBase); // Saves a copy of the base config directory
		cspm.configure(getSource(configFile), new ConfigFinder(null, configBase), true);
		this.setConfigBase(configBase);
		this.setConfigFile(configFile);
		result = cspm;
		
		return result;
	}

	protected void setConfigBase(File configBase) {
		this.configBase = configBase;
	}
	
	protected File getConfigBase() {
		return this.configBase;
	}
	
	protected void setConfigFile(File configFile) {
		this.configFile = configFile;
	}
	
	protected File getConfigFile() {
		return this.configFile;
	}
	
	boolean areXsdSnippetsEqual(String source, String target) {
		return MakeXsd.areXsdSnippetsEqual(getConfigFile().getName(), source, target);
	}

}
