package org.collectionspace.chain.csp.webui.authorities;

import java.util.Iterator;

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
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VocabulariesRead implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(VocabulariesRead.class);
	private Instance n;
	private String base;
	
	public VocabulariesRead(Instance n) {
		this.base=n.getID();
		this.n=n;
	}

	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub

	}

	/**
	 * Returns all the objects that are linked to a vocabulary item
	 * @param storage
	 * @param path
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	private JSONArray getTermsUsed(Storage storage,String path) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONArray out=new JSONArray();
		JSONObject mini = storage.retrieveJSON(path);
		if(mini != null){
			log.debug("mini="+mini);
			Iterator t=mini.keys();
			while(t.hasNext()) {
				String field=(String)t.next();
				JSONObject in=mini.getJSONObject(field);
				JSONObject entry=new JSONObject();
				entry.put("csid",in.getString("csid"));
				entry.put("recordtype",in.getString("recordtype"));
				entry.put("sourceFieldName",field);
				entry.put("number",in.getString("displayName"));
				out.put(entry);
			}
		}
		
		return out;
	}
	
	/* Wrapper exists to decomplexify exceptions */
	private JSONObject getJSON(Storage storage,String csid) throws UIException {
		JSONObject out=new JSONObject();
		try {
			JSONObject fields=storage.retrieveJSON(n.getRecord().getID()+"/"+n.getTitleRef()+"/"+csid);
			
			String refPath = n.getRecord().getID()+"/"+n.getTitleRef()+"/"+csid+"/refObjs";
			
			fields.put("csid",csid);
			//JSONArray relations=createRelations(storage,csid);
			out.put("fields",fields);
			out.put("relations",new JSONArray());
			//out.put("relations",relations);
			out.put("termsUsed",getTermsUsed(storage,refPath));
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem getting",e);
		} catch (JSONException e) {
			throw new UIException("Could not create JSON"+e,e);
		}
		if (out == null) {
			throw new UIException("No JSON Found");
		}
		return out;
	}
	
	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		// Get the data
		JSONObject outputJSON = getJSON(storage,path);
		try {
			outputJSON.put("csid",path);
		} catch (JSONException e1) {
			throw new UIException("Cannot add csid",e1);
		}
		// Write the requested JSON out
		request.sendJSONResponse(outputJSON);
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
