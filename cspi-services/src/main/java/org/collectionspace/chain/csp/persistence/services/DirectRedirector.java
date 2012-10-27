/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.services.common.api.RefName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectRedirector implements ContextualisedStorage {
	private static final Logger log=LoggerFactory.getLogger(DirectRedirector.class);
	private Spec spec;

	DirectRedirector(Spec spec) { this.spec=spec; }
	
	@Override
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject jsonObject, JSONObject restrictions)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}

	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}
	
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject, JSONObject restrictions) 
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}
	
	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, 
			String filePath, String workflowTransition) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String path, JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		/* Find relevant controller, and call */
		String[] url=path.split("/");
        RefName.AuthorityItem itemParsed = RefName.AuthorityItem.parse(url[2]);
        String thisShortid = itemParsed.getShortIdentifier();
        String thisparent = itemParsed.getParentShortIdentifier();
        String displayName = itemParsed.displayName;
        String test = itemParsed.inAuthority.resource;

		String vocab = RefName.shortIdToPath(thisparent);
		String csid = RefName.shortIdToPath(thisShortid);
		
		Record r=spec.getRecordByServicesUrl(itemParsed.inAuthority.resource);
		String storageID = r.getID();
		
		if(r.isType("vocabulary")){
			url[0] = r.getServicesURL();
			storageID = "vocab";
		}
		if(!r.isType("authority") && !r.isType("vocabulary"))
			throw new UnimplementedException("Only authorities and vocabularies supported at direct at the moment");
		return root.retrieveJSON(root,creds,cache,storageID+"/_direct/"+url[0]+"/"+vocab+"/"+csid, restrictions);
	}
}
