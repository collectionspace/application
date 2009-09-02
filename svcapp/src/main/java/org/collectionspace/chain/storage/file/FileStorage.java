/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.storage.file;

import java.io.File;
import java.io.IOException;

import org.collectionspace.chain.storage.ProxyStorage;
import org.collectionspace.chain.storage.SplittingStorage;
import org.collectionspace.chain.storage.Storage;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class FileStorage extends ProxyStorage implements Storage {
	private String root;
	
	public FileStorage(String root) throws IOException {
		this.root=root;
		File data=new File(root,"data");
		if(!data.exists())
			data.mkdir();
		super.setTarget(new StubJSONStore(data.getCanonicalPath()));
	}
	
	public String getStoreRoot() { return root; }
}
