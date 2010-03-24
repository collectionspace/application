package org.collectionspace.chain.csp.webui.userdetails;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;

/**
 * receive JSON: { email: "fred@bloggs.com" }
 * test if valid user
 * if valid user: create token and email to user
 * if not send back exception
 * @author csm22
 *
 */
public class UserDetailsReset implements WebMethod {
	
	private void store_reset(Storage storage,UIRequest request,String path) throws UIException {
		
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_reset(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}
 

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}
