package org.collectionspace.chain.config.main.csp;

import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

// XXX call order
public class CoreConfig implements CSP, ConfigConsumer {

	public void go(CSPContext ctx) {
		ctx.addConfigConsumer(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) {
		// Set up "main" content provider/consumer
		SimpleBarbWirer main=new SimpleBarbWirer("main");
		ctx.getRootBarbWirer().getAttachmentPoint("root").attach(main,"collection-space");
		main.addAttachmentPoint("persistence",new String[]{"persistence"});
		// XXX move out
		SimpleConfigProviderBarbWirer persistence=new SimpleConfigProviderBarbWirer(new Object[]{"persistence","services"});		
		ctx.addConfigProvider(persistence);
		main.getAttachmentPoint("persistence").attach(persistence,"services");
	
	}
}
