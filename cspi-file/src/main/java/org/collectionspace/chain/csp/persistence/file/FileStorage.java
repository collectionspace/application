/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import java.io.File;
import java.io.IOException;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.Configurable;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.config.SimpleConfigProviderBarbWirer;
import org.collectionspace.csp.helper.persistence.ProxyStorage;
import org.collectionspace.kludge.*;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class FileStorage extends ProxyStorage implements Storage, CSP, ConfigConsumer, Configurable {
	private String root;

	public FileStorage() {}
	FileStorage(String root) throws IOException, CSPDependencyException { real_init(root); } // For testing

	public String getStoreRoot() { return root; }

	public String getName() { return "persistence.file"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("file",this);
		ctx.addConfigConsumer(this);
		ctx.addConfigurable(this);
	}

	public void prepareForConfiguration(ConfigContext ctx) throws CSPDependencyException {
		BarbWirer main=ctx.getRootBarbWirer().getBarb("root").getBarbWirer("collection-space");
		if(main==null) {
			throw new CSPDependencyException("No collection-space tag attached to root");
		}
		SimpleConfigProviderBarbWirer persistence=new SimpleConfigProviderBarbWirer(new Object[]{"persistence","file"});		
		ctx.addConfigProvider(persistence);
		main.getBarb("persistence").attach(persistence,"file");
	}

	private void real_init(String root) throws CSPDependencyException {
		this.root=root;
		File data=new File(root,"data");
		if(!data.exists())
			data.mkdir();
		try {
			super.setTarget(new StubJSONStore(data.getCanonicalPath()));
		} catch (IOException e) {
			throw new CSPDependencyException("Could not set target"); // XXX wrong type
		}
	}

	public void configure(CRKludge config) throws CSPDependencyException {
		Object store=config.getValue(new Object[]{"bootstrap","store"});
		if(store!=null && (store instanceof String)) {
			real_init((String)store);
		} else {
			store=config.getValue(new Object[]{"persistence","file","store"});
			if(store==null || !(store instanceof String))
				return;
			real_init((String)store);
		}
	}
}
