package org.collectionspace.chain.installation;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.test.TestConfigFinder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class TestServices {
	private static final Logger log = LoggerFactory.getLogger(TestServices.class);

	private InputStream getSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream();
		} catch (CSPDependencyException e) {
			String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
		}
	}
	
	private Spec getSpec(){

		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		try {
			cspm.go();
			cspm.configure(new InputSource(getSource("config.xml")),null);
		} catch (CSPDependencyException e) {
			log.error("CSPManagerImpl failed");
			log.error(e.getLocalizedMessage() );
		}
		

		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		return spec;
		
		
	}
	
	@Test public void testServices(){
	
		Services bob = new Services(getSpec());
	
		String tester = bob.doit();
		log.info(tester);
	}
}
