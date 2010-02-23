/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import java.io.File;
import java.io.IOException;

import org.collectionspace.bconfigutils.bootstrap.BootstrapCSP;
import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigController;
import org.collectionspace.chain.csp.config.Configurable;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.csp.config.Target;
import org.collectionspace.chain.csp.inner.CoreConfig;
import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.StorageGenerator;
import org.collectionspace.csp.helper.persistence.ProxyStorage;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class FileStorage extends ProxyStorage implements Storage, CSP, Configurable, StorageGenerator {
	public static String SECTION_PREFIX="org.collectionspace.app.config.persistence.file.";
	public static String FILE_ROOT=SECTION_PREFIX+"spec";
	
	private String root;
	private CSPContext ctx;

	public FileStorage() {}
	FileStorage(String root) throws IOException, CSPDependencyException { 
		this.root=root;
		real_init();
	} // For testing

	public String getStoreRoot() { return root; }

	public String getName() { return "persistence.file"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("file",this);
		ctx.addConfigRules(this);
		this.ctx=ctx;
	}

	private void real_init() throws CSPDependencyException {
		File data=new File(root,"data");
		if(!data.exists())
			data.mkdir();
		try {
			super.setTarget(new StubJSONStore(data.getCanonicalPath()));
		} catch (IOException e) {
			throw new CSPDependencyException("Could not set target"); // XXX wrong type
		}
	}

	public Storage getStorage(CSPRequestCache cache) { return this; }

	public void configure(Rules rules) throws CSPDependencyException {
		/* MAIN/persistence/file -> FILE */
		rules.addRule("org.collectionspace.app.cfg.main",new String[]{"persistence","file"},SECTION_PREFIX+"file",null,new Target(){
			public Object populate(Object parent, ReadOnlySection milestone) {
				((CoreConfig)parent).setRoot(FILE_ROOT,FileStorage.this);
				root=(String)milestone.getValue("/store");
				return FileStorage.this;
			}
		});	
	}
	public void config_finish() throws CSPDependencyException {
		BootstrapConfigController bootstrap=(BootstrapConfigController)ctx.getConfigRoot().getRoot(BootstrapCSP.BOOTSTRAP_ROOT);
		String boot_root=bootstrap.getOption("store");
		if(boot_root!=null)
			root=boot_root;
		real_init();
	}
}
