/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import java.io.File;
import java.io.IOException;

import org.collectionspace.csp.api.core.CSP;
import org.collectionspace.csp.api.core.CSPContext;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.persistence.ProxyStorage;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class FileStorage extends ProxyStorage implements Storage, CSP {
	private String root;
	
	public FileStorage(String root) throws IOException {
		this.root=root;
		File data=new File(root,"data");
		if(!data.exists())
			data.mkdir();
		super.setTarget(new StubJSONStore(data.getCanonicalPath()));
	}
	
	public String getStoreRoot() { return root; }

	public String getName() { return "persistence.file"; }

	public void go(CSPContext ctx) throws CSPDependencyException {
		ctx.addStorageType("file",this);
	}
}
