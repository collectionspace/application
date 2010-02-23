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
	
	public String getName() { return "schema"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	public void configure(Rules rules) {
		/* MAIN/spec -> SPEC */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"spec"},SECTION_PREFIX+"spec",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(SECTION_PREFIX+"spec",Spec.this);
				return Spec.this;
			}
		});
		/* SPEC/records -> RECORDS */
		rules.addRule(SECTION_PREFIX+"spec",new String[]{"records"},SECTION_PREFIX+"records",null,null);
		/* RECORDS/record -> RECORD(@id) */
		rules.addRule(SECTION_PREFIX+"records",new String[]{"record"},SECTION_PREFIX+"record",null,new Target(){
			public Object populate(Object parent, ReadOnlySection section) {
				Record r=new Record(section);
				records.put(r.getID(),r);
				return r;
			}
		});
	}

	public void config_finish() {}
}
