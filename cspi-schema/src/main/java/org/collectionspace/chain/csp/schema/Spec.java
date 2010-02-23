package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.CoreConfig;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

public class Spec implements CSP, NConfigurable {
	public static String SECTION_PREFIX="org.collectionspace.app.config.spec.";
	public static String SPEC_ROOT=SECTION_PREFIX+"spec";
	
	private Map<String,Record> records=new HashMap<String,Record>();
	
	public String getName() { return "schema"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigRules(this);
	}

	public void configure(ConfigRoot config) throws CSPDependencyException {
		System.err.println(config.dump());
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		BarbWirer main=ctx.getRootBarbWirer().getBarb("root").getBarbWirer("collection-space");
		if(main==null) {
			throw new CSPDependencyException("No collection-space tag attached to root");
		}
		SimpleConfigProviderBarbWirer persistence=new SimpleConfigProviderBarbWirer(new Object[]{"records"});
		ctx.addConfigProvider(persistence);
		main.getBarb("misc").attach(persistence,"records");
	}

	public void nconfigure(Rules rules) {
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
