package org.collectionspace.chain.csp.webui.relate;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Only complexity is that we delete opposite directions */

public class RelateDelete implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RelateDelete.class);
	private boolean one_way;
	
	public RelateDelete(boolean one_way) {
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
		// XXX CSPACE-1834 need to support pagination
		JSONObject data = storage.getPathsJSON("relations/main",restrictions);
		String[] relations = (String[]) data.get("listItems");
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
			throw new UIException("Exist exception deleting ",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception deleting ",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		} catch (JSONException e) {
			throw new UIException("Exception building JSON ",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		relate_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
