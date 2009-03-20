package org.collectionspace.xxu.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.collectionspace.xxu.api.ConfigLoadingMessages;
import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.CSPDependency;
import org.collectionspace.xxu.api.CSPMetadata;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.collectionspace.xxu.csp.transform.CSPProviderTransformImpl;
import org.junit.Test;

public class CSPTest {
	private static final String[] standard_result={
			"start:config",
			"start:config,field",
			"start:config,field,@id",
			"text:config,field,@id:first",
			"end:config,field,@id",
			"start:config,field,validation",
			"start:config,field,validation,@id",
			"text:config,field,validation,@id:1",
			"end:config,field,validation,@id",
			"end:config,field,validation",
			"end:config,field",
			"start:config,field",
			"start:config,field,@id",
			"text:config,field,@id:second",
			"end:config,field,@id",
			"start:config,field,validation",
			"start:config,field,validation,@id",
			"text:config,field,validation,@id:2",
			"end:config,field,validation,@id",
			"end:config,field,validation",
			"end:config,field",
			"end:config"
	};
	
	private File file_from_resources(String in) throws Exception {
		URL url=Thread.currentThread().getContextClassLoader().getResource(in);
		if(url==null)
			throw new IOException("No such resource "+in);
		File out=new File(url.getFile());
		if(out==null || !out.exists())
			throw new IOException("No such resource "+in);
		return out;
	}

	private void config_from_resources(ConfigLoader cfg,String name) throws Exception {
		File f=file_from_resources(name);
		cfg.loadConfigFromXML(new FileInputStream(f),f);
	}

	
	@Test public void testCSPSmoke() throws Exception {
		ConfigLoader loader=new ConfigLoaderImpl();
		loader.loadCSPFromDirectory(file_from_resources("csp-test-one"));
	}

	@Test public void testCSPMetadata() throws Exception {
		ConfigLoader loader=new ConfigLoaderImpl();
		CSP csp=loader.loadCSPFromDirectory(file_from_resources("csp-test-one"));
		CSPMetadata md=csp.getMetadata();
		assertEquals("test-one",md.getIdentity());
		assertEquals(12,md.getMajorVersion());
		assertEquals(13,md.getMinorVersion());	
		assertEquals("Test One",md.getHumanID());
		assertEquals("Test One CSP for Unit Tests",md.getHumanTitle());
		assertEquals("Dan Sheppard",md.getAuthor());
		assertEquals("http://www.caret.cam.ac.uk/",md.getAuthorURL());
		assertEquals("http://www.collectionspace.org/",md.getCSPURL());
		assertEquals("This is a Test CSP which is used in the unit tests.",md.getHumanDescription());
		CSPDependency[][] cspd=md.getDependencies();
		assertEquals(3,cspd.length);
		assertEquals(1,cspd[0].length);
		assertEquals(1,cspd[1].length);
		assertEquals(2,cspd[2].length);
		assertEquals("test-four",cspd[2][1].getIdentity());
		assertEquals(1,cspd[2][1].getMajorVersion());
		assertEquals(0,cspd[2][1].getMinimumMinorVersion());
	}
	
	private void metadataMustBeBad(String in) throws Exception {
		try {
			new ConfigLoaderImpl().loadCSPFromDirectory(file_from_resources("csp-test-bad-version"));
			assertTrue(false);
		} catch(ConfigLoadingException x) {}		
	}
	
	public @Test void testCSPRestOptional() throws Exception {
		new ConfigLoaderImpl().loadCSPFromDirectory(file_from_resources("csp-rest-optional"));
	}
	
	@Test public void testCSPTransformerProvider() throws Exception {
		CSP csp=new ConfigLoaderImpl().loadCSPFromDirectory(file_from_resources("csp-test-one"));
		CSPProvider[] prov=csp.getMetadata().getProvider();
		assertEquals(2,prov.length);
		assertTrue(prov[0] instanceof CSPProviderTransformImpl);
	}
	
	@Test public void testCSPBadVersion() throws Exception { metadataMustBeBad("csp-test-bad-version"); }
	@Test public void testCSPBadIdentity() throws Exception { metadataMustBeBad("csp-test-bad-identity"); }
	@Test public void testCSPBadVersion1() throws Exception { metadataMustBeBad("csp-test-bad-version1"); }
	@Test public void testCSPBadVersion2() throws Exception { metadataMustBeBad("csp-test-bad-version2"); }
	@Test public void testCSPBadVersion3() throws Exception { metadataMustBeBad("csp-test-bad-version3"); }
	@Test public void testCSPBadShortName() throws Exception { metadataMustBeBad("csp-test-bad-short-name"); }
	
	@Test public void testXSLTProvider() throws Exception {
		XMLEventTester tester=new XMLEventTester(standard_result);
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		cfg.setEventConsumerForTesting(tester);
		CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-test-one"));
		cfg.addCSP(csp);
		config_from_resources(cfg,"config.xml");
	}	
	
	@Test public void testAttachmentMechanism() throws Exception {
		String[] xslt_field={
				"start:field",
				"start:field,@id",
				"text:field,@id:first",
				"end:field,@id",

				"end:field",
				"start:field",
				"start:field,@id",
				"text:field,@id:second",
				"end:field,@id",

				"end:field",
		};
		
		String[] xslt_validation={
				"start:validation",
				"start:validation,@id",
				"text:validation,@id:1",
				"end:validation,@id",
				"end:validation",

				"start:validation",
				"start:validation,@id",
				"text:validation,@id:2",
				"end:validation,@id",
				"end:validation",
		};
				
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		BasicFieldConsumerForTest field_tester=new BasicFieldConsumerForTest(cfg,xslt_field);
		XMLEventTester validation_tester=new XMLEventTester(xslt_validation);
		cfg.registerAttachment("root","field",field_tester);
		cfg.registerAttachmentPoint("root",new String[]{"field"},"field");
		cfg.registerAttachment("field","validation",validation_tester);
		CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-test-two"));
		cfg.addCSP(csp);
		config_from_resources(cfg,"config.xml");
	}
	
	@Test public void testAttachmentSmoke() throws Exception {
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-attach-field"));
		cfg.addCSP(csp);
		config_from_resources(cfg,"config.xml");
	}
	
	@Test public void testAttachmentDual() throws Exception {
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		CSP csp1=cfg.loadCSPFromDirectory(file_from_resources("csp-attach-field"));
		cfg.addCSP(csp1);
		CSP csp2=cfg.loadCSPFromDirectory(file_from_resources("csp-attach-validation"));
		cfg.addCSP(csp2);
		config_from_resources(cfg,"config.xml");
	}
	
	@Test public void testConfigSmoke() throws Exception {
		ConfigLoader cfg=new ConfigLoaderImpl();
		config_from_resources(cfg,"config.xml");
	}
	
	@Test public void testConfigInclude() throws Exception {
		XMLEventTester tester=new XMLEventTester(standard_result);
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		cfg.setEventConsumerForTesting(tester);
		CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-test-one"));
		cfg.addCSP(csp);
		config_from_resources(cfg,"config-include.xml");
	}
	
	@Test public void testCSPInclude() throws Exception {
		XMLEventTester tester=new XMLEventTester(standard_result);
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		cfg.setEventConsumerForTesting(tester);
		CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-xinclude"));
		cfg.addCSP(csp);
		config_from_resources(cfg,"config.xml");
	}
	
	@Test public void testConfigError() throws Exception {
		ConfigLoaderImpl cfg=new ConfigLoaderImpl();
		ConfigLoadingMessages msg=new ConfigMessagesTester();
		cfg.setMessages(msg);
		try {
			config_from_resources(cfg,"config-bad.xml");
			assertTrue(false);
		} catch(ConfigLoadingException e) {
			String cmp="Error loading config. See messages for details: summary";
			assertEquals(cmp,e.getMessage().substring(0,cmp.length()));
		}
	}
	
	@Test public void testCSPXSLTError() throws Exception {
		try {
			ConfigLoaderImpl cfg=new ConfigLoaderImpl();
			ConfigLoadingMessages msg=new ConfigMessagesTester();
			cfg.setMessages(msg);
			CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-bad-xslt"));
			cfg.addCSP(csp);
			config_from_resources(cfg,"config.xml");		
		} catch(ConfigLoadingException e) {
			String cmp="Error loading config. See messages for details: summary";
			assertEquals(cmp,e.getMessage().substring(0,cmp.length()));
		}
	}

	@Test public void testCSPError() throws Exception {
		try {
			ConfigLoaderImpl cfg=new ConfigLoaderImpl();
			ConfigLoadingMessages msg=new ConfigMessagesTester();
			cfg.setMessages(msg);
			CSP csp=cfg.loadCSPFromDirectory(file_from_resources("csp-test-bad-identity"));
			cfg.addCSP(csp);
			config_from_resources(cfg,"config.xml");		
		} catch(ConfigLoadingException e) {
			String cmp="Error loading config. See messages for details: summary";
			assertEquals(cmp,e.getMessage().substring(0,cmp.length()));
		}
	}
	
}
