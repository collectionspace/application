/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.collectionspace.csp.helper.core.ConfigFinder;
import org.collectionspace.csp.helper.test.TestConfigFinder;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSchema {

	private static final Logger log=LoggerFactory.getLogger(TestSchema.class);
	
	private InputSource getSource(String fallbackFile) {
		try {
			return TestConfigFinder.getConfigStream();
		} catch (CSPDependencyException e) {
			String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+fallbackFile;
			return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
		}
	}
	
	@Test public void testSchema()  {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		try {
			cspm.go();
			InputSource configsource = getSource("config.xml");
			cspm.configure(configsource,new ConfigFinder(null));
		} catch (CSPDependencyException e) {
			log.error("CSPManagerImpl failed");
			log.error(e.getLocalizedMessage() );
		}
		

		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		Record r_obj=spec.getRecord("collection-object");

		assertNotNull(r_obj);
		assertEquals("collection-object",r_obj.getID());
		assertEquals("cataloging",r_obj.getWebURL());
	
		
		/* RECORD/field -> FIELD(type) */
		/*
		rules.addRule("record",new String[]{"field"},"field",new SectionGenerator() {
			public void step(Section milestone,Map<String, String> data) {
				milestone.addValue("field.type",milestone.getParent().getValue("/record/type"));
			}
		},new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				Field f=new Field();
				f.type=(String)milestone.getValue("field.type");
				((Record)parent).fields.add(f);
				return f;
			}
		});
			
		// MAIN/persistence/service -> URL(url)
		rules.addRule("main",new String[]{"persistence","service"},"url",new SectionGenerator() {
			public void step(Section milestone,Map<String, String> data) {
				milestone.addValue("service.url",data.get("/service/url"));
			}
		},null);
		*/
		
	}
}
