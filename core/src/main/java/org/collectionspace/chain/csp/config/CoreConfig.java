package org.collectionspace.chain.csp.config;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.nconfig.NConfigRoot;
import org.collectionspace.chain.csp.nconfig.NConfigurable;
import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.Target;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

// XXX call order DependencyNotSatisfiedException
public class CoreConfig implements CSP, NConfigurable, NConfigRoot {
	private Map<String,Object> roots=new HashMap<String,Object>();
	
	public void go(CSPContext ctx) { 
		ctx.addConfigRules(this);
		ctx.setNConfigRoot(this);
		ctx.addConfigRules(this);
	}
	
	public String getName() { return "config.core"; }

	public void nconfigure(Rules rules) {
		/* ROOT/collection-space -> MAIN */
		rules.addRule("ROOT",new String[]{"collection-space"},"org.collectionspace.app.cfg.main",null,new Target() {
			public Object populate(Object parent, ReadOnlySection milestone) {
				return CoreConfig.this;
			}			
		});
	}
	
	public void setRoot(String key,Object value) { roots.put(key,value); }
	public Object getRoot(String key) { return roots.get(key); }

	public void config_finish() {}
}
