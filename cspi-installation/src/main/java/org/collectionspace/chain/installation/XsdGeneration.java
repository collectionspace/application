package org.collectionspace.chain.installation;

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
	private String file = "";
	

	public XsdGeneration(String configfile, String record, String domain, String type) throws UIException {

		CSPManager cspm=getServiceManager(configfile);
		Spec spec = getSpec(cspm);
		//valid recordtype?
		//log.info("record:"+record);
		if(spec.hasRecordByServicesUrl(record)){
			Record tryme = spec.getRecordByServicesUrl(record);
			//log.info("TYPE"+type);
			if(type.equals("core")){
				MakeXsd catlog = new MakeXsd( getTenantData(cspm), domain);
				file = catlog.serviceschema(domain, tryme);
			}
			else if(type.equals("delta")){
				Services tenantbob = new Services(getSpec(cspm), getTenantData(cspm),false);
				file = tenantbob.doit();
			}
		}
	}
	public String getFile(){
		return file;
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
