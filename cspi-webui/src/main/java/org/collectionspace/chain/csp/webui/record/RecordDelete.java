package org.collectionspace.chain.csp.webui.record;

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


public class RecordDelete implements WebMethod {
	private String base;
	
	public RecordDelete(String base) { this.base=base; }

	private void store_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			storage.deleteJSON(base+"/"+path);
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem getting",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}
