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
import org.json.JSONException;
import org.json.JSONObject;

/* Only complexity is that we delete opposite directions */

public class WebRelateDelete implements WebMethod {
	private boolean one_way;
	
	public WebRelateDelete(boolean one_way) {
		this.one_way=one_way;
	}
	
	public void configure(WebUI ui, Spec spec) {}

	// XXX factor
	private String findReverse(Storage storage,String csid_fwd) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		// What's our destination
		JSONObject obj_fwd=storage.retrieveJSON("/relations/main/"+csid_fwd);
		// Find a backward record
		JSONObject restrictions=new JSONObject();
		restrictions.put("dst",obj_fwd.getString("src"));
		restrictions.put("src",obj_fwd.getString("dst"));
		restrictions.put("type",obj_fwd.getString("type")); // XXX what about non-self-inverses?
		String[] relations=storage.getPaths("relations/main",restrictions);
		if(relations.length==0)
			return null;
		return relations[0];
	}
	
	private void relate_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			if(!one_way) {
				String rev=findReverse(storage,path);
				if(rev!=null)
					storage.deleteJSON("/relations/main/"+rev);
			}
			storage.deleteJSON("/relations/main/"+path);
			request.setOperationPerformed(Operation.DELETE);
		} catch (ExistException e) {
			throw new UIException("Exist exception deleting "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception deleting "+e,e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Underlying storage exception deleting "+e,e);
		} catch (JSONException e) {
			throw new UIException("Exception building JSON "+e,e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		relate_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
