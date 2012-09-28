/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.relate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Record;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX non-self-inverse relations

public class RelateCreateUpdate implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(RelateCreateUpdate.class);
	private boolean create;
	private Map<String,String> url_to_type=new HashMap<String,String>();

	public RelateCreateUpdate(boolean create) {
		this.create=create;
	}


	private JSONObject createServicesObject(String src_type,String src,String type,String dst_type,String dst) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("src",url_to_type.get(src_type)+"/"+src);
		out.put("dst",url_to_type.get(dst_type)+"/"+dst);
		out.put("type",type);
		return out;
	}
	private JSONObject convertPayload(JSONObject in,boolean reverse) throws JSONException { // XXX check for errors
		JSONObject source=in.getJSONObject("source");
		JSONObject target=in.getJSONObject("target");
		if(reverse) {
			JSONObject tmp=source;
			source=target;
			target=tmp;
		}
		String type=in.getString("type");		
		return createServicesObject(source.getString("recordtype"),source.getString("csid"),type,target.getString("recordtype"),target.getString("csid"));
	}

	public String sendJSONOne(Storage storage,String path,JSONObject data,boolean reverse) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject fields=convertPayload(data,reverse);
		if(path!=null) {
			// Update
			if(fields!=null)
				storage.updateJSON("/relations/main/"+path,fields, new JSONObject());
		} else {
			// Create
			if(fields!=null)
				path=storage.autocreateJSON("/relations/main",fields,null);
		}
		return path;
	}

	private String find_reverse(Storage storage,String csid_fwd) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		// What's our destination
		JSONObject obj_fwd=storage.retrieveJSON("/relations/main/"+csid_fwd, new JSONObject());
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
	
	private void relate_one(Storage storage,JSONObject data,String path,boolean reverse) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		if(path==null) {
			path=sendJSONOne(storage,null,data,reverse);
		} else
			path=sendJSONOne(storage,path,data,reverse);
		if(path==null)
			throw new UIException("Insufficient data for create (no fields?)");
		data.put("csid",path);
	}
	
	private void relate(Storage storage,UIRequest request,String path) throws UIException {
		try {
			JSONObject data=request.getJSONBody();
			boolean and_reverse=false;
			if(!data.optBoolean("one-way"))
				and_reverse=true;
			if(data.has("type")) {
				// Single
				if(!create) {
					// Update may mean tinkering with other relations because of two-way-ness
					/* NOTE at the moment there's no support in the service to indicate partner, so we assume that
					 * an opposite relation is evidence of a two way relationship. This creates a potential bug if
					 * the user has set up two independent one way relationships and wants to maintain them
					 * independently. It's arguable that this is the behaviour they expect, arguable that it is not.
					 */
					// Delete the reverse record for an update
					log.debug("forward is "+path);
					String csid_rev=find_reverse(storage,path);
					log.debug("reverse is "+csid_rev);
					if(csid_rev!=null)
						storage.deleteJSON("/relations/main/"+csid_rev);
				}
				relate_one(storage,data,path,false);
				if(and_reverse) {
					/* If we're not one way and we're updating, create reverse (we just deleted the old one) */
					relate_one(storage,data,null,true);
				}
			} else if(data.has("items")) {
				// Multiple
				JSONArray relations=data.getJSONArray("items");
				for(int i=0;i<relations.length();i++) {
					JSONObject itemdata = relations.getJSONObject(i);
					if(!create)
						throw new UIException("Cannot use multiple syntax for update");

					if(!itemdata.optBoolean("one-way")){
						and_reverse=true;
					}
					relate_one(storage,relations.getJSONObject(i),path,false);					
					if(and_reverse)
						relate_one(storage,relations.getJSONObject(i),path,true);
				}
			} else
				throw new UIException("Bad JSON data");

			request.sendJSONResponse(data);
			request.setOperationPerformed(create?Operation.CREATE:Operation.UPDATE);
			if(create)
				request.setSecondaryRedirectPath(new String[]{"relationships",path}); // XXX should be derivable
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: ",x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: ",x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: ",x);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
	}

	public void configure(WebUI ui, Spec spec) {
		url_to_type = spec.ui_url_to_id();
		//for(Record r : spec.getAllRecords()) {
		//	url_to_type.put(r.getWebURL(),r.getID());
		//}
	}

	public void configure(Spec spec) {
		url_to_type = spec.ui_url_to_id();
		//for(Record r : spec.getAllRecords()) {
		//	url_to_type.put(r.getWebURL(),r.getID());
		//}
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		String path=StringUtils.join(tail,"/");
		if(create)
			path=null;
		relate(q.getStorage(),q.getUIRequest(),path);
	}
}
