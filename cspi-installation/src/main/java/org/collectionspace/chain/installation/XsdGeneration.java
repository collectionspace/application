package org.collectionspace.chain.installation;

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
		
		//valid recordtype?
		//log.info("record:"+record);
		if(spec.hasRecordByServicesUrl(recordType)){
			Record tryme = spec.getRecordByServicesUrl(recordType);
			//log.info("TYPE"+type);
			if(type.equals("core")){
				MakeXsd catlog = new MakeXsd(getTenantData(cspm));
				serviceSchemas = catlog.serviceschemas(domain, tryme, recordType, schemaVersion);
			} else if(type.equals("delta")){
				Services tenantbob = new Services(getSpec(cspm), getTenantData(cspm),false);
				tenantBindings = tenantbob.doit();
			}
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
