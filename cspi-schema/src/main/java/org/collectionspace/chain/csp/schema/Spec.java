package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;

public class Spec implements CSP, Configurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.spec.";
	public static String SPEC_ROOT=SECTION_PREFIX+"spec";
	
	private Map<String,Record> records=new HashMap<String,Record>();
	private Map<String,Record> records_by_web_url=new HashMap<String,Record>();
	
	public String getName() { return "schema"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	public void configure(Rules rules) {
		/* MAIN/spec -> SPEC */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"spec"},SECTION_PREFIX+"spec",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(SPEC_ROOT,Spec.this);
				return Spec.this;
			}
		});
		/* SPEC/records -> RECORDS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"records"},SECTION_PREFIX+"records",null,null);
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule(SECTION_PREFIX+"records",new String[]{"record"},SECTION_PREFIX+"record",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Record r=new Record(Spec.this,section);
				records.put(r.getID(),r);
				records_by_web_url.put(r.getWebURL(),r);
				return r;
			}
		});
		/* RECORD/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=new Field((Record)parent,section);
				((Record)parent).addField(f);
				String is_chooser=(String)section.getValue("/@chooser");
				if(is_chooser!=null && ("1".equals(is_chooser) || "yes".equals(is_chooser.toLowerCase())))
					f.setType("chooser");
				return f;
			}
		});		
		/* RECORD/repeat -> REPEAT */
		rules.addRule(SECTION_PREFIX+"record",new String[]{"repeat"},SECTION_PREFIX+"repeat",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Repeat r=new Repeat((Record)parent,section);
				((Record)parent).addField(r);
				return r;
			}
		});
		
		/* REPEAT/field -> FIELD */
		rules.addRule(SECTION_PREFIX+"repeat",new String[]{"field"},SECTION_PREFIX+"field",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field r=new Field((Repeat)parent,section);
				((Repeat)parent).addChild(r);
				return r;
			}
		});		
		
		/* FIELD/options/option -> OPTION */
		rules.addRule(SECTION_PREFIX+"field",new String[]{"options","option"},SECTION_PREFIX+"option",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Field f=(Field)parent;
				boolean dfault=false;
				String value=(String)section.getValue("/@default");
				dfault=(value!=null && ("yes".equals(value.toLowerCase()) || "1".equals(value.toLowerCase())));
				f.addOption((String)section.getValue("/@id"),(String)section.getValue(""),(String)section.getValue("/@sample"),dfault);
				return f;
			}
		});		
	}

	public Record getRecord(String id) { return records.get(id); }
	public Record getRecordByWebUrl(String url) { return records_by_web_url.get(url); }
	public Record[] getAllRecords() { return records.values().toArray(new Record[0]); }
	
	public void config_finish() {}
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		for(Record r : records.values())
			r.dump(out);
		return out.toString();
	}
}
