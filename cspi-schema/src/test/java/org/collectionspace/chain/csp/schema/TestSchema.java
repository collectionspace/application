package org.collectionspace.chain.csp.schema;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.csp.config.ConfigRoot;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.container.impl.CSPManagerImpl;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TestSchema {
	private InputStream getSource(String file) {
		String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+file;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}
	
	@Test public void testSchema() throws Exception {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Spec());
		cspm.go();
		cspm.configure(new InputSource(getSource("config.xml")),null);
		ConfigRoot root=cspm.getConfigRoot();
		Spec spec=(Spec)root.getRoot(Spec.SPEC_ROOT);
		assertNotNull(spec);
		System.err.println(spec.dump());
		Record r_obj=spec.getRecord("collection-object");
		assertNotNull(r_obj);
		assertEquals("collection-object",r_obj.getID());
		assertEquals("objects",r_obj.getWebURL());
		
		
		
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
