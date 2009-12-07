package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.csp.helper.persistence.SplittingStorage;

public class ServicesStorageGenerator extends SplittingStorage implements ContextualisedStorage, StorageGenerator, CSP, ConfigConsumer, Configurable {

	public Storage getStorage(CSPRequestCache cache) {
		return new ServicesStorage(this,cache);
	}

	public String getName() { return "persistence.services"; }

	private void real_init(String base_url) throws CSPDependencyException {
		try {
			ServicesConnection conn=new ServicesConnection(base_url);
			addChild("collection-object",new ServicesCollectionObjectStorage(conn));
			addChild("intake",new ServicesIntakeStorage(conn));
			addChild("acquisition",new ServicesAcquisitionStorage(conn));
			addChild("id",new ServicesIDGenerator(conn));
		} catch (Exception e) {
			throw new CSPDependencyException("Could not set target",e); // XXX wrong type
		}
	}
	
	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("service",this);
		ctx.addConfigConsumer(this);
		ctx.addConfigurable(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		BarbWirer main=ctx.getRootBarbWirer().getBarb("root").getBarbWirer("collection-space");
		if(main==null) {
			throw new CSPDependencyException("No collection-space tag attached to root");
		}
		SimpleConfigProviderBarbWirer persistence=new SimpleConfigProviderBarbWirer(new Object[]{"persistence","service"});
		ctx.addConfigProvider(persistence);
		main.getBarb("persistence").attach(persistence,"service");
	}

	public ServicesStorageGenerator() {}
	public ServicesStorageGenerator(String store) throws CSPDependencyException {
		real_init(store);
	}
	
	public void configure(ConfigRoot config) throws CSPDependencyException { // XXX
		Object store=config.getValue(new Object[]{"bootstrap","store-url"});
		if(store!=null && (store instanceof String)) {
			real_init((String)store);
		} else {
			store=config.getValue(new Object[]{"persistence","services","url"});
			if(store==null || !(store instanceof String))
				return;
			real_init((String)store);
		}
	}
}
