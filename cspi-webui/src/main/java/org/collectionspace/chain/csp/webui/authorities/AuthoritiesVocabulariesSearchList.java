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
	
	private JSONObject generateMiniRecord(Storage storage,String auth_type,String inst_type,String csid) throws JSONException {

		JSONObject out = new JSONObject();
		try{
			String postfix = "list";
			if(this.search){
				postfix = "search";
			}
			out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view/"+postfix, new JSONObject());
			out.put("csid",csid);
			out.put("recordtype",inst_type);
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
	
	private JSONObject setRestricted(UIRequest ui, String param, String pageNum, String pageSize) throws UIException, JSONException{

		JSONObject returndata = new JSONObject();

		JSONObject restriction=new JSONObject();
		String key="results";
		
		Set<String> args = ui.getAllRequestArgument();
		for(String restrict : args){
			if(!restrict.equals("_")){
				if(ui.getRequestArgument(restrict)!=null){
					String value = ui.getRequestArgument(restrict);
					if(restrict.equals("sortDir")){
						restriction.put(restrict,value);
					}
					else if(restrict.equals("sortKey")){////"summarylist.updatedAt"//movements_common:locationDate
						String[] bits = value.split("\\.");
						String fieldname = value;
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
							fs = r.getFieldFullList(fieldname);
						}
						fieldname = fs.getID();
						FieldSet tmp = fs;
						while(!(tmp.getParent() instanceof Record)){
							tmp = (FieldSet)tmp.getParent();
							if(!tmp.getSearchType().equals("repeator")){
								fieldname = tmp.getServicesParent()[0] +"/0/"+fieldname;
							}
						}

						String tablebase = r.getServicesRecordPath(fs.getSection()).split(":",2)[0];
						String newvalue = tablebase+":"+fieldname;
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
		else{
			restriction.put("pageNum","0");
		}
		if(pageSize!=null){
			restriction.put("pageSize",pageSize);
		}
		if(param==null){
			key = "items";
		}
		returndata.put("key", key);
		returndata.put("restriction", restriction);
		return returndata;
	}
	
	private void advancedSearch(Storage storage,UIRequest ui,JSONObject restriction, String resultstring, JSONObject params) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException{
		
			Map<String, String> dates = new HashMap<String, String>();
			
			String operation = params.getString("operation").toUpperCase();
			JSONObject fields = params.getJSONObject("fields");
			
			String asq = ""; 
			Iterator rit=fields.keys();
			while(rit.hasNext()) {
				String join = " ILIKE "; //using ilike so we can have case insensitive searches
				String fieldname=(String)rit.next();
				Object item = fields.get(fieldname);

				String value = "";
				
				if(item instanceof JSONArray){ // this is a repeatable
					JSONArray itemarray = (JSONArray)item;
					for(int j=0;j<itemarray.length();j++){
						JSONObject jo = itemarray.getJSONObject(j);
						Iterator jit=jo.keys();
						while(jit.hasNext()){
							String jname=(String)jit.next();
							if(!jname.equals("_primary")){
								if(jo.get(jname) instanceof String || jo.get(jname) instanceof Boolean ){
									value = jo.getString(jname);
									asq += getAdvancedSearch(jname,value,operation,join);
								}
							}
						}
					}
					
				}
				else if(item instanceof JSONObject){ // no idea what this is
					
				}
				else if(item instanceof String){
					value = (String)item;
					if(!value.equals("")){
						String fieldid = fieldname;
						if(this.r.hasSearchField(fieldname) && this.r.getSearchFieldFullList(fieldname).getUIType().equals("date")){
							if(fieldname.endsWith("Start")){
								fieldid = fieldname.substring(0, (fieldname.length() - 5));
								join = ">= DATE ";
							}
							else if(fieldname.endsWith("End")){
								fieldid = fieldname.substring(0, (fieldname.length() - 3));
								join = "<= DATE ";
							}

							if(dates.containsKey(fieldid)){
								String temp = getAdvancedSearch(fieldid,value,"AND",join);
								String get = dates.get(fieldid);
								dates.put(fieldid, temp + get);
							}
							else{
								String temp = getAdvancedSearch(fieldid,value,"",join);
								dates.put(fieldid, temp);
							}
						}
						else{
							asq += getAdvancedSearch(fieldname,value,operation,join);
						}
					}
				}
			}

			if(!dates.isEmpty()){
				for (String keyed : dates.keySet()) {
					if(!dates.get(keyed).equals("")){
						asq += " ( "+dates.get(keyed)+" )  "+ operation;	
					}
				}
			}
			
			if(!asq.equals("")){
				asq = asq.substring(0, asq.length()-(operation.length() + 2));
			}
			asq = asq.trim();
			if(!asq.equals("")){
				String asquery = "( "+asq+" )";
				restriction.put("advancedsearch", asquery);
			}
			resultstring="results";

			search_or_list(storage,ui,restriction,resultstring);
		
	}

	private String getAdvancedSearch(String fieldname, String value, String operator, String join){
		if(!value.equals("")){
			try{
				String section = this.r.getFieldFullList(fieldname).getSection();
				String spath=this.r.getServicesRecordPath(section);
				String[] parts=spath.split(":",2);
				if(value.contains("*")){
					value = value.replace("*", "%");
					join = " ilike ";
				}
				//backslash quotes??
				
				return parts[0]+":"+fieldname+join+"\""+value +"\""+ " " + operator+ " ";
			}
			catch(Exception e){
				return "";
			}
		}
		return "";
	}
	private void search_or_list_vocab(JSONObject out,Instance n,Storage storage,UIRequest ui,JSONObject restriction, String resultstring, JSONObject temp ) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UIException {
		
		JSONObject data = storage.getPathsJSON(r.getID()+"/"+n.getTitleRef(),restriction);

		
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
	
	private void search_or_list(Storage storage,UIRequest ui,JSONObject restriction, String resultstring) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
			
			JSONObject results=new JSONObject();
			if(n==null) {
				for(Instance n : r.getAllInstances()) {
					JSONObject results2=new JSONObject();
					search_or_list_vocab(results2,n,storage,ui,restriction,resultstring,results);
					results = results2;
				}
			} else {
				search_or_list_vocab(results,n,storage,ui,restriction,resultstring,new JSONObject());				
			}
			ui.sendJSONResponse(results);
	}

	public void searchtype(Storage storage,UIRequest ui,String param, String pageSize, String pageNum) throws UIException{

		try {

			JSONObject restrictedkey = setRestricted(ui,param,pageNum,pageSize);
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
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		if(search)
			searchtype(q.getStorage(),q.getUIRequest(),q.getUIRequest().getRequestArgument("query"),q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
		else
			searchtype(q.getStorage(),q.getUIRequest(),null,q.getUIRequest().getRequestArgument("pageSize"),q.getUIRequest().getRequestArgument("pageNum"));
		
	}

	public void configure(WebUI ui,Spec spec) {
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}
}
