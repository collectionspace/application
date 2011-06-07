/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
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

public class AuthoritiesVocabulariesSearchList implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(AuthoritiesVocabulariesSearchList.class);
	private Record r;
	private Instance n;
	private boolean search;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	//search all instances of an authority
	public AuthoritiesVocabulariesSearchList(Record r,boolean search) {
		this.r=r;
		this.search=search;
	}

	//search a specific instance of an authority
	public AuthoritiesVocabulariesSearchList(Instance n,boolean search) {
		this.n=n;
		this.r=n.getRecord();
		this.search=search;
	}
	
	private JSONObject generateMiniRecord(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		out.put("csid",csid);
		out.put("recordtype",inst_type);
		out.put("number",out.get(r.getDisplayNameField().getID()));
		out.put("summary",out.get(r.getDisplayNameField().getID())); // XXX proper summary!?
		return out;		
	}
	
	
	
	private void search_or_list_vocab(JSONObject out,Instance n,Storage storage,UIRequest ui,String param, String pageSize, String pageNum, JSONObject temp ) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException {
		JSONObject restriction=new JSONObject();
		String resultstring = "results";
		
		Set<String> args = ui.getAllRequestArgument();
		for(String restrict : args){
			if(!restrict.equals("_")){
				if(ui.getRequestArgument(restrict)!=null){
					String value = ui.getRequestArgument(restrict);
					if(restrict.equals("sortDir")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortKey")){////"summarylist.updatedAt"//movements_common:locationDate
						String[] bits = value.split("\\.");						String fieldname = value;
						if(bits.length>1){
							fieldname = bits[1];
						}
						FieldSet fs = null;
						if(fieldname.equals("number")){
							fs = r.getMiniNumber();
						}
						else if(fieldname.equals("summary")){
							fs = r.getMiniSummary();
						}
						else{
							//convert sortKey
							fs = r.getField(fieldname);
						}
						String tablebase = r.getServicesRecordPath(fs.getSection()).split(":",2)[0];
						String newvalue = tablebase+":"+bits[1];
						restriction.put(restrict,newvalue);
					}
				}
			}
		}
		
		
		if(param!=null && !param.equals("")){
			restriction.put("queryTerm", "kw");
			restriction.put("queryString",param);
			//restriction.put(r.getDisplayNameField().getID(),param);
		}
		if(pageNum!=null){
			restriction.put("pageNum",pageNum);
		}
		if(pageSize!=null){
			restriction.put("pageSize",pageSize);
		}
		JSONObject data = storage.getPathsJSON(r.getID()+"/"+n.getTitleRef(),restriction);

		if(param==null){
			resultstring = "items";
		}
		
		String[] paths = (String[]) data.get("listItems");
		JSONObject pagination = new JSONObject();
		if(data.has("pagination")){
			pagination = data.getJSONObject("pagination");
		}
		
		JSONArray members = new JSONArray();
		/* Get a view of each */ 
		if(temp.has(resultstring)){
			members = temp.getJSONArray(resultstring);
		}
		for(String result : paths) {
			
			if(temp.has(resultstring)){
				temp.getJSONArray(resultstring).put(generateMiniRecord(storage,r.getID(),n.getTitleRef(),result));
				members = temp.getJSONArray(resultstring);
			}
			else{
				members.put(generateMiniRecord(storage,r.getID(),n.getTitleRef(),result));
			}
		}

		out.put(resultstring,members);
		
		if(pagination!=null){
			if(temp.has("pagination")){
				JSONObject pag2 = temp.getJSONObject("pagination");
				String itemsInPage = pag2.getString("itemsInPage");
				String pagSize = pag2.getString("pageSize");
				String totalItems = pag2.getString("totalItems");
				
				String itemsInPage1 = pagination.getString("itemsInPage");
				String pagSize1 = pagination.getString("pageSize");
				String totalItems1 = pagination.getString("totalItems");
				int iip = Integer.parseInt(itemsInPage) +Integer.parseInt(itemsInPage1);
				int ps = Integer.parseInt(pagSize) +Integer.parseInt(pagSize1);
				int ti = Integer.parseInt(totalItems) +Integer.parseInt(totalItems1);
				pagination.put("itemsInPage", Integer.toString(iip) );
				pagination.put("pageSize", Integer.toString(ps) );
				pagination.put("totalItems", Integer.toString(ti) );
				
			}
			out.put("pagination",pagination);
		}
		log.debug(restriction.toString());
	}
	
	private void search_or_list(Storage storage,UIRequest ui,String param, String pageSize, String pageNum) throws UIException {
		try {
			JSONObject results=new JSONObject();
			if(n==null) {
				for(Instance n : r.getAllInstances()) {
					JSONObject results2=new JSONObject();
					search_or_list_vocab(results2,n,storage,ui,param,pageSize,pageNum,results);
					results = results2;
				}
			} else {
				search_or_list_vocab(results,n,storage,ui,param,pageSize,pageNum,new JSONObject());				
			}
			ui.sendJSONResponse(results);
		} catch (JSONException e) {
			throw new UIException("Cannot generate JSON",e);
		} catch (ExistException e) {
			throw new UIException("Exist exception",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			ui.sendJSONResponse(uiexception.getJSON());
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			search_or_list(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"),q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
		else
			search_or_list(q.getStorage(),q.getUIRequest(),null,q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
