package org.collectionspace.chain.csp.webui.main;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONObject;

public class WebRelateDelete implements WebMethod {

	public void configure(WebUI ui, Spec spec) {}

	// XXX two way
	private void relate_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			String[] ids=storage.getPaths("/relations/main",new JSONObject());
			for(String id : ids)
				storage.deleteJSON("/relations/main/"+id);
			request.setOperationPerformed(Operation.DELETE);
		} catch (ExistException e) {
			throw new UIException("Exist exception deleting "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception deleting "+e,e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Underlying storage exception deleting "+e,e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		relate_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
