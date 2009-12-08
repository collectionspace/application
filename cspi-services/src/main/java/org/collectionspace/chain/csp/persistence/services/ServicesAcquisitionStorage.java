package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;

import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.DocumentException;

public class ServicesAcquisitionStorage extends GenericRecordStorage implements ContextualisedStorage {
	
	public ServicesAcquisitionStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		super(conn,"acquisition.jxj","acquisition","acquisitions",
			  "acquisitions_common","acquisitions-common-list/acquisition-list-item",
			  new String[]{"AcquisitionReferenceNumber"},new String[]{"acquReferenceNum"});
	}
}
