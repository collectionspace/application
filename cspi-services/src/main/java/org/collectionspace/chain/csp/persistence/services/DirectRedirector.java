package org.collectionspace.chain.csp.persistence.services;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.json.JSONObject;

public class DirectRedirector implements ContextualisedStorage {
	private Spec spec;

	DirectRedirector(Spec spec) { this.spec=spec; }
	
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject jsonObject)
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

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) 
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("direct uses get only");
	}
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String path)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		/* Find relevant controller, and call */
		String[] url=path.split("/");
		Record r=spec.getRecordByServicesUrl(url[1]);
		if(!r.isType("authority"))
			throw new UnimplementedException("Only authorities supported at direct at the moment");
		return root.retrieveJSON(root,creds,cache,r.getID()+"/_direct/"+url[0]+"/"+url[2]+"/"+url[4]+"/"+url[5]);
	}
}
