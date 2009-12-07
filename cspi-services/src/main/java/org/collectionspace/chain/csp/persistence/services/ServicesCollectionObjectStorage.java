/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.DocumentException;

class ServicesCollectionObjectStorage extends GenericRecordStorage implements ContextualisedStorage {
	public ServicesCollectionObjectStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		super(conn,"collectionobject.jxj","collection-object","collectionobjects",
				  "collectionobjects_common","collectionobjects-common-list/collection-object-list-item",
				  new String[]{"objectNumber"},new String[]{"accessionNumber"});

	}
}
