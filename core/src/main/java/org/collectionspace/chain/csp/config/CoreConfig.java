package org.collectionspace.chain.csp.config;

import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.container.CSPManager;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

// XXX call order DependencyNotSatisfiedException
public class CoreConfig implements CSP, ConfigConsumer {

	public void go(CSPContext ctx) { ctx.addConfigConsumer(this); }

	public void prepareForConfiguration(ConfigContext ctx) {
		// Set up "main" content provider/consumer
		SimpleBarbWirer main=new SimpleBarbWirer("main");
		ctx.getRootBarbWirer().getBarb("root").attach(main,"collection-space");
		main.addAttachmentPoint("persistence",new String[]{"persistence"});	
		main.addAttachmentPoint("ui",new String[]{"ui"});
		main.addAttachmentPoint("misc",new String[0]);
	}
	
	public String getName() { return "config.core"; }
}
