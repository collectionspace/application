/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.misc.Generic;
import org.collectionspace.chain.csp.webui.misc.GenericSearch;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.Operation;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthoritiesVocabulariesSearchList implements WebMethod {
	final static String VOCAB_WILDCARD = "_ALL_";
	private static final Logger log=LoggerFactory.getLogger(AuthoritiesVocabulariesSearchList.class);
	private Record r;
	private Instance n;
	private boolean search;
	private Map<String,String> type_to_url=new HashMap<String,String>();
	
	//search all instances of an authority
	public AuthoritiesVocabulariesSearchList(Record r,boolean search) {
		this.r=r;
		this.n=null;
		this.search=search;
	}

	//search a specific instance of an authority
	public AuthoritiesVocabulariesSearchList(Instance n,boolean search) {
		this.n=n;
		this.r=n.getRecord();
		this.search=search;
	}
	
	private JSONObject generateMiniRecord(Storage storage,String auth_type,String inst_type,String csid) throws JSONException {

		JSONObject out = new JSONObject();
		try{
			StringBuilder sb = new StringBuilder();
			sb.append(auth_type);
			sb.append("/");
			sb.append(inst_type);
			sb.append("/");
			sb.append(csid);
			sb.append((this.search)?"/view/search":"/view/list");
			String path = sb.toString();
			out=storage.retrieveJSON(path, new JSONObject());
			out.put("csid",csid);
			// Record type should be set properly from list results.
			//out.put("recordtype",inst_type);
		}
		catch (ExistException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "Exist Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} catch (UnimplementedException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "Unimplemented  Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} catch (UnderlyingStorageException e) {
			out.put("csid",csid);
			out.put("isError", true);
			JSONObject msg = new JSONObject();
			msg.put("severity", "error");
			msg.put("message", "UnderlyingStorage Exception:"+e.getMessage());
			JSONArray msgs = new JSONArray();
			msgs.put(msg);
			out.put("messages", msgs);
		} 
		return out;		
	}
	
	
	private void advancedSearch(Storage storage,UIRequest ui,JSONObject restriction, String resultstring, JSONObject params) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException{
		GenericSearch.buildQuery(this.r,params, restriction);
		resultstring="results";
		search_or_list(storage,ui,restriction,resultstring);
	}

	
	private void search_or_list_vocab(JSONObject out,Record rd,Instance n,Storage storage,JSONObject restriction, String resultstring, JSONObject temp ) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException {
		
		JSONObject data = storage.getPathsJSON(rd.getID()+"/"+n.getTitleRef(),restriction);

		
		String[] paths = (String[]) data.get("listItems");
		JSONObject pagination = new JSONObject();
		if(data.has("pagination")){
			pagination = data.getJSONObject("pagination");
			pagination.put("numInstances", "1" );
		}
		
		JSONArray members = new JSONArray();
		/* Get a view of each */ 
		if(temp.has(resultstring)){
			members = temp.getJSONArray(resultstring);
		}
		for(String result : paths) {
			
			if(temp.has(resultstring)){
				temp.getJSONArray(resultstring).put(generateMiniRecord(storage,rd.getID(),n.getTitleRef(),result));
				members = temp.getJSONArray(resultstring);
			}
			else{
				members.put(generateMiniRecord(storage,rd.getID(),n.getTitleRef(),result));
			}
		}

		out.put(resultstring,members);
		
		if(pagination!=null){
			if(temp.has("pagination")){
				JSONObject pag2 = temp.getJSONObject("pagination");
				String itemsInPage = pag2.getString("itemsInPage");
				String pagSize = pag2.getString("pageSize");
				String totalItems = pag2.getString("totalItems");
				String numInstances = pag2.getString("numInstances");
				
				String itemsInPage1 = pagination.getString("itemsInPage");
				String pagSize1 = pagination.getString("pageSize");
				String totalItems1 = pagination.getString("totalItems");
				Integer numInstances1 = Integer.parseInt(numInstances);
				int iip = Integer.parseInt(itemsInPage) +Integer.parseInt(itemsInPage1);
				int ps = Integer.parseInt(pagSize) +Integer.parseInt(pagSize1);
				int ti = Integer.parseInt(totalItems) +Integer.parseInt(totalItems1);
				
				pagination.put("itemsInPage", Integer.toString(iip) );
				pagination.put("pageSize", Integer.toString(ps) );
				pagination.put("totalItems", Integer.toString(ti) );
				pagination.put("numInstances", Integer.toString(numInstances1++) );
				
			}
			out.put("pagination",pagination);
		}
		log.debug(restriction.toString());
	}

	private void search_or_list_all_vocabs(JSONObject out, Record rd, Storage storage,
			JSONObject restriction, String resultstring ) 
			throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException {
		
		JSONObject data = storage.getPathsJSON(rd.getID(),restriction);
		
		String[] paths = (String[]) data.get("listItems");
		JSONObject pagination = new JSONObject();
		if(data.has("pagination")){
			pagination = data.getJSONObject("pagination");
		}
		
		JSONArray members = new JSONArray();
		for(String result : paths) {
			// The storage will use the wildcard if we get on this record with no vocab subpath,
			// so each item comes back into the cache with that URL
			members.put(generateMiniRecord(storage,rd.getID(),VOCAB_WILDCARD,result));
		}

		out.put(resultstring,members);
		
		if(pagination!=null){
			out.put("pagination",pagination);
			int numInstances = rd.getNumInstances();
			pagination.put("numInstances", Integer.toString(numInstances) );
		}
		log.debug(restriction.toString());
	}

	private void search_or_list(Storage storage,UIRequest ui,JSONObject restriction, String resultstring) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
			
			JSONObject results = getJSON(storage, restriction, resultstring);
			//cache for record traverser
			if(results.has("pagination") && results.getJSONObject("pagination").has("separatelists")){
				String nid = (this.n == null)?"":this.n.getID();
				GenericSearch.createTraverser(ui, this.r.getID(), nid, results, restriction, resultstring, Integer.valueOf(results.getJSONObject("pagination").getString("numInstances")));
			}
			ui.sendJSONResponse(results);
	}

	/* Wrapper exists to be used inRead, hence not private */
	public JSONObject getJSON(Storage storage, JSONObject restriction,
			String resultstring) throws ExistException,
			UnimplementedException, UnderlyingStorageException, JSONException,
			UIException {
		JSONObject results = new JSONObject();
		if(this.n==null) {
			/*
			for(Instance n : this.r.getAllInstances()) {
				JSONObject results2=new JSONObject();
				search_or_list_vocab(results2,this.r, n,storage,restriction,resultstring,results);
				results = results2;
			}
			*/
			search_or_list_all_vocabs(results,this.r,storage,restriction,resultstring);
		} else {
			search_or_list_vocab(results,this.r,this.n,storage,restriction,resultstring,new JSONObject());				
		}
		return results;
	}
	

	public void searchtype(Storage storage,UIRequest ui,String param, String pageSize, String pageNum) throws UIException{

		try {

			JSONObject restrictedkey = GenericSearch.setRestricted(ui,param,pageNum,pageSize,search, this.r);
			JSONObject restriction = restrictedkey.getJSONObject("restriction");
			String resultstring = restrictedkey.getString("key");
			
			if(ui.getBody() == null || StringUtils.isBlank(ui.getBody())){
				search_or_list(storage,ui,restriction,resultstring);
			}
			else{
				//advanced search
				advancedSearch(storage,ui,restriction,resultstring, ui.getJSONBody());
			}

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
	
	@Override
	public void run(Object in, String[] tail) throws UIException {
		if(tail.length > 0) {
			throw new UIException("Illegal search specified. Tail: "+r.getWebURL()+"/"+StringUtils.join(tail,"/"));
		}
		Request q=(Request)in;
		UIRequest uir = q.getUIRequest();
		if(search) {
			searchtype(q.getStorage(),uir,
					uir.getRequestArgument(SEARCH_QUERY_PARAM),
					uir.getRequestArgument(PAGE_SIZE_PARAM),
					uir.getRequestArgument(PAGE_NUM_PARAM));
		} else {
			searchtype(q.getStorage(), uir, null,
					uir.getRequestArgument(PAGE_SIZE_PARAM),
					uir.getRequestArgument(PAGE_NUM_PARAM));
		}
		
	}

	@Override
	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
