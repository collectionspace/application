package org.collectionspace.chain.csp.webui.authorities;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VocabulariesCreateUpdate implements WebMethod {
	private boolean create;
	private Instance n;
	private  VocabulariesRead reader;
	
	public VocabulariesCreateUpdate(Instance n,boolean create) {
		this.create=create;
		this.n=n;
		reader=new VocabulariesRead(n);
	}
	
	
	public void configure(WebUI ui, Spec spec) {}
	
	private String sendJSON(Storage storage,String path,JSONObject data) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=data.optJSONObject("fields");
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON(n.getRecord().getID()+"/"+n.getTitleRef()+"/"+path,fields);
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON(n.getRecord().getID()+"/"+n.getTitleRef(),fields);
		}
		
		// XXX no vobaulary relations for now. Naming is too complex.
		return path;
	}
	
	private void store_set(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=request.getJSONBody();
			if(create) {
				path=sendJSON(storage,null,data);
				// JIRA CSPACE-1173 - is there a better way to do this? Should be used cached data at least
				String path1=n.getRecord().getID()+"/"+n.getTitleRef();
				//JSONObject minirecord = storage.retrieveJSON(path1 +"/"+path+"/view");
				//data.put("urn", minirecord.get("refid")); //sibling of csid
				//data.getJSONObject("fields").put("urn", minirecord.get("refid")); 
			} else
				path=sendJSON(storage,path,data);
			if(path==null)
				throw new UIException("Insufficient data for create (no fields?)");
			
			data=reader.getJSON(storage,path);
			String refid = data.getJSONObject("fields").getString("refid");
			data.put("urn", refid);
			data.getJSONObject("fields").put("urn", refid);
			data.put("csid",data.getJSONObject("fields").getString("csid"));
			
			request.sendJSONResponse(data);
			request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			if(create)
				request.setSecondaryRedirectPath(new String[]{n.getWebURL(),path});
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new UIException("Problem storing: "+x,x);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_set(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
