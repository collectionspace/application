/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
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

	private String  getRelationShipID(Storage storage,String source,String target,String type) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{

		JSONObject restrictions=new JSONObject();
		restrictions.put("dst",target);
		restrictions.put("src",source);
		restrictions.put("type",type); // XXX what about non-self-inverses?
		// XXX CSPACE-1834 need to support pagination
		JSONObject data = storage.getPathsJSON("relations/main",restrictions);
		String[] relations = (String[]) data.get("listItems");
		if(relations.length==0)
			return null;
		return relations[0];
	}
	
	// XXX factor
	private String findReverse(Storage storage,String csid_fwd) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		// What's our destination
		JSONObject obj_fwd=storage.retrieveJSON("/relations/main/"+csid_fwd, new JSONObject());
		// Find a backward record
		return getRelationShipID(storage, obj_fwd.getString("dst"), obj_fwd.getString("src"), obj_fwd.getString("type"));
		
	}
	
	private void relate_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			String source = request.getRequestArgument(RELATION_SOURCE_PARAM);
			String target = request.getRequestArgument(RELATION_TARGET_PARAM);
			String type = request.getRequestArgument(RELATION_TYPE_PARAM);
			String oneway = request.getRequestArgument(RELATION_ONE_WAY_PARAM);
			if(oneway !=null && oneway !=""){
				one_way = Boolean.parseBoolean(oneway);
			}

			if(request.isJSON()){
				JSONObject data=request.getJSONBody();
				if(data.has("source")&&data.has("target")&&data.has("type")){
					source = data.getJSONObject("source").getString("recordtype") + "/" + data.getJSONObject("source").getString("csid");
					target = data.getJSONObject("target").getString("recordtype") + "/" + data.getJSONObject("target").getString("csid");
					type = data.getString("type");
					if(data.has("one-way")){
						one_way = data.getBoolean("one-way"); 
					}
				}
			}
			
			if(source != null && target != null && type != null && source !="" && target !="" &&  type !=""){
				path = getRelationShipID(storage, source,target,type);
				if(!one_way){ // easier to find reverse if have actually sub items
					String reverse = getRelationShipID(storage, target, source, type);
					if(reverse!=null)
						storage.deleteJSON("/relations/main/"+reverse);
				}
			}
			else{
				//find csids' if delete sent with just relationship csid rather than sub items
				if(!one_way) {
					String rev=findReverse(storage,path);
					if(rev!=null)
						storage.deleteJSON("/relations/main/"+rev);
				}
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
