/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.storage.file;

import org.collectionspace.chain.storage.SplittingStorage;
import org.collectionspace.chain.storage.Storage;

/**  SplittingStorage which delegates collection-objects to StubJSONStore
 * 
 */
public class FileStorage extends SplittingStorage implements Storage {

	public FileStorage(String root) {
		addChild("collection-object",new StubJSONStore(root));
	}
}
