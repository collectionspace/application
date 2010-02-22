package org.collectionspace.chain.csp.schema;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;

public class Schema implements CSP, Configurable, ConfigConsumer {

	public String getName() { return "schema"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addConfigConsumer(this);
		ctx.addConfigurable(this);
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

}
