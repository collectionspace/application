/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.persistence.SplittingStorage;
import org.collectionspace.kludge.BCCKludge;
import org.collectionspace.kludge.CRKludge;
import org.dom4j.DocumentException;

/** The direct implementation of storage; only an instance of SplittingStorage which at the moment only splits
 * into ServicesCollectionObjectStorage.
 * 
 */
public class ServicesStorage extends SplittingStorage implements CSP, Storage, ConfigConsumer, Configurable {

	public ServicesStorage() {}
	ServicesStorage(String base_url) throws CSPDependencyException { // For testing
		real_init(base_url);
	}

	public String getName() { return "persistence.services"; }

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

	private void real_init(String base_url) throws CSPDependencyException {
		try {
			ServicesConnection conn=new ServicesConnection(base_url);
			addChild("collection-object",new ServicesCollectionObjectStorage(conn));
		} catch (Exception e) {
			throw new CSPDependencyException("Could not set target"); // XXX wrong type
		}
	}
	
	public void configure(BCCKludge bootstrap,CRKludge config) throws CSPDependencyException { // XXX
		String bs=bootstrap.getOption("store-url");
		if(bs!=null) {
			real_init((String)bs);
		} else {
			Object store=config.getValue(new Object[]{"persistence","services","url"});
			if(store==null || !(store instanceof String))
				return;
			real_init((String)store);
		}
	}
}
