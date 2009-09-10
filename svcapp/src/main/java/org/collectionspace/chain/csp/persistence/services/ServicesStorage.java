/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.helper.persistence.SplittingStorage;
import org.dom4j.DocumentException;

/** The direct implementation of storage; only an instance of SplittingStorage which at the moment only splits
 * into ServicesCollectionObjectStorage.
 * 
 */
public class ServicesStorage extends SplittingStorage {

	public ServicesStorage(String base_url) throws InvalidJXJException, DocumentException, IOException {
		ServicesConnection conn=new ServicesConnection(base_url);
		addChild("collection-object",new ServicesCollectionObjectStorage(conn));
	}
}
