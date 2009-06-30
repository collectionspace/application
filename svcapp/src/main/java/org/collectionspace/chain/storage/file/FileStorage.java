package org.collectionspace.chain.storage.file;

import org.collectionspace.chain.storage.SplittingStorage;
import org.collectionspace.chain.storage.Storage;

public class FileStorage extends SplittingStorage implements Storage {

	public FileStorage(String root) {
		addChild("collection-object",new StubJSONStore(root));
	}
}
