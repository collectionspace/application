package org.collectionspace.chain.csp.schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.chain.csp.nconfig.Section;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.SectionGenerator;
import org.collectionspace.chain.csp.nconfig.impl.main.Rule;
import org.collectionspace.chain.csp.nconfig.impl.parser.ConfigParser;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.impl.core.CSPManagerImpl;
import org.junit.Test;
import org.xml.sax.InputSource;

public class TestSchema {
	
	private static class Field {
		private String type;
	}
	
	private static class Record {
		private List<Field> fields=new ArrayList<Field>();
		private String id,type;
	}
	
	private InputStream getSource(String file) {
		String name=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+file;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}
	
	@Test public void testSchema() throws Exception {
		CSPManager cspm=new CSPManagerImpl();
		cspm.register(new CoreConfig());
		cspm.register(new Schema());
		cspm.go();
		
		final List<Record> records=new ArrayList<Record>();
		
		Rules rules=new Rules();
		/* ROOT/collection-space -> MAIN */
		rules.addRule("ROOT",new String[]{"collection-space"},"main",null,null);
		/* MAIN/records -> RECORDS */
		rules.addRule("main",new String[]{"records"},"records",null,null);
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule("records",new String[]{"record"},"record",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				Record r=new Record();
				r.id=(String)milestone.getValue("/record/@id");
				r.type=(String)milestone.getValue("/record/type");
				records.add(r);
				return r;
			}
		});
		/* RECORD/field -> FIELD(type) */
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
			
		/* MAIN/persistence/service -> URL(url) */
		rules.addRule("main",new String[]{"persistence","service"},"url",new SectionGenerator() {
			public void step(Section milestone,Map<String, String> data) {
				milestone.addValue("service.url",data.get("/service/url"));
			}
		},null);
				
		ConfigParser parser=new ConfigParser(rules);
		parser.parse(new InputSource(getSource("config.xml")),null);
		
		for(Record r : records) {
			System.err.println("Record type="+r.type+" id="+r.id);
			for(Field f : r.fields) {
				System.err.println("Field type="+f.type);
			}
		}
	}
}
