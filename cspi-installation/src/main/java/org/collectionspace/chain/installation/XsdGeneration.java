package org.collectionspace.chain.installation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

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
	private HashMap<String, String> serviceSchemas = new HashMap<String, String>();
	private String tenantBindings = null;
	
	public HashMap<String, String> getServiceSchemas() {
		return serviceSchemas;
	}
	
	public XsdGeneration(String configfile, String recordType, String domain, String type, String schemaVersion) throws UIException {

		String current = null;
		try {
			current = new java.io.File( "." ).getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("Current dir:"+current);
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current dir using System:" +currentDir);
        
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
						serviceSchemas.put(definedSchema, definedSchemaList.get(definedSchema));
						log.trace(String.format("New Services schema '%s' defined in configuration record '%s'.",
								definedSchema, record.getID()));
					} else {
						// Otherwise, it already exists so emit a warning just in case it shouldn't have been redefined.
						log.warn(String.format("Services schema '%s' defined in record '%s', but was previously defined in another record.",
								definedSchema, record.getID()));
						log.trace(String.format("Redefined services schema is: %s", definedSchemaList.get(definedSchema)));
					}
				}
			} else if (type.equals("delta")) { // Create the service bindings.
				Services tenantbob = new Services(getSpec(cspm), getTenantData(cspm),false);
				tenantBindings = tenantbob.doit();
			}
		} else {
			log.error(String.format("Record type '%s' is unknown in configuration file", configfile));
		}
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
