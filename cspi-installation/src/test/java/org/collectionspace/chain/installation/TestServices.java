package org.collectionspace.chain.installation;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class TestServices {
	private static final Logger log = LoggerFactory.getLogger(TestServices.class);

	private InputSource getSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream(fallbackFile, true);
		} catch (CSPDependencyException e) {
			String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
			return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
		}
	}
	
	private Spec getSpec(CSPManager cspm){
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
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
			cspm.configure(getSource(filename),new ConfigFinder(null),false);
		} catch (CSPDependencyException e) {
			log.error("CSPManagerImpl failed");
			log.error(e.getLocalizedMessage() );
		}
		return cspm;
		
	}
	@Test 
	public void testServices(){

		String configfile = "testsci-tenant.xml";
		String recordtype = "collectionobjects"; //these are service names for the record/procedure
		String domain = "collectionspace_core"; // this is either domain or collectionspace_core
		String maketype = "core"; // this is either delta or core

		log.info("new system");
//		try {
//			XsdGeneration s = new XsdGeneration(configfile, recordtype, domain, maketype);
//			log.info(s.getFile());
//		} catch (UIException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
		maketype = "delta"; //this might not be delta - but it will be one day
//		try {
//			XsdGeneration s = new XsdGeneration(configfile, recordtype, domain, maketype);
//			log.info(s.getFile());
//		} catch (UIException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
	}
}
