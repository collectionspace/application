package org.collectionspace.chain.csp.config;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

public class ServicesConfig implements CSP, ConfigConsumer {

	public void go(CSPContext ctx) { ctx.addConfigConsumer(this); }

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		BarbWirer main=ctx.getRootBarbWirer().getBarb("root").getBarbWirer("collection-space");
		if(main==null) {
			throw new CSPDependencyException("No collection-space tag attached to root");
		}
		SimpleConfigProviderBarbWirer persistence=new SimpleConfigProviderBarbWirer(new Object[]{"persistence","services"});		
		ctx.addConfigProvider(persistence);
		main.getBarb("persistence").attach(persistence,"services");
	}
	
	public String getName() { return "config.core"; }
}
