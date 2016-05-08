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

import org.collectionspace.services.common.api.RefName;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Relationship;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.record.RecordAuthorities;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.services.common.api.Tools;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VocabulariesRead implements WebMethod {
	public static final int GET_FULL_INFO = 0;
	public static final int GET_BASIC_INFO = 1;
	public static final int GET_TERMS_USED_INFO = 2;
	public static final int GET_REF_OBJS_INFO = 3;
	private static final Logger log=LoggerFactory.getLogger(VocabulariesRead.class);
	private Instance n;
	private String base;
	private Spec spec;
	private RecordAuthorities termsused;
	private int getInfoMode;
	private Map<String,String> type_to_url=new HashMap<String,String>();

	public VocabulariesRead(Instance n) {
		this(n, GET_FULL_INFO);
	}
	public VocabulariesRead(Instance n, int getInfoMode) {
		this.getInfoMode = getInfoMode;
		this.base=n.getID();
		this.n=n;
		if(this.spec == null){
			this.spec = n.getRecord().getSpec();
			for(Record r : spec.getAllRecords()) {
				type_to_url.put(r.getID(),r.getWebURL());
			}
		}
		Record r =n.getRecord();
		this.termsused = new RecordAuthorities(r);
	}
	
	public void configure(WebUI ui,Spec spec) {
		this.spec = spec;
		for(Record r : spec.getAllRecords()) {
			type_to_url.put(r.getID(),r.getWebURL());
		}
	}

	/**
	 * Returns all the Authorities that are associated to a vocabulary item
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
		JSONObject mini=storage.retrieveJSON(path+"/refs", new JSONObject());
		JSONArray out=new JSONArray();
		Iterator t=mini.keys();
		while(t.hasNext()) {
			String field=(String)t.next();
			if(mini.get(field) instanceof JSONArray){
				JSONArray array = (JSONArray)mini.get(field);
				for(int i=0;i<array.length();i++) {
					JSONObject in = array.getJSONObject(i);
					JSONObject entry=getTermsUsedData(in);
					out.put(entry);
				}
			}
			else{
				JSONObject in=mini.getJSONObject(field);
				JSONObject entry=getTermsUsedData(in);
				out.put(entry);
			}
		}
		return out;
	}	
	
	private JSONObject getTermsUsedData(JSONObject in) throws JSONException{
		JSONObject entry=new JSONObject();
		entry.put("csid",in.getString("csid"));
		entry.put("recordtype",in.getString("recordtype"));
		//entry.put("sourceFieldName",field);
		entry.put("sourceFieldselector",in.getString("sourceFieldselector"));
		entry.put("sourceFieldName",in.getString("sourceFieldName"));
		entry.put("sourceFieldType",in.getString("sourceFieldType"));
		
		entry.put("number",in.getString("displayName"));
		return entry;
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
	private void getRefObjs(Storage storage,String path, JSONObject out, String itemsKey, 
			boolean addPagination, JSONObject restrictions) throws JSONException   {
		JSONArray items=new JSONArray();
		try{
			JSONObject refObjs = storage.retrieveJSON(path+"/refObjs", restrictions);
			if(refObjs != null) {
				if(refObjs.has("items")) {
					JSONArray ritems = refObjs.getJSONArray("items");
					for(int i=0;i<ritems.length();i++) {
						JSONObject in = ritems.getJSONObject(i);
						//JSONObject in=ritems.getJSONObject(field);
						String rt = in.getString("sourceFieldType");
						Record rec = this.spec.getRecordByServicesDocType(rt);
						// CSPACE-6184
						//
						// The Services doctypes for certain authority term records
						// (e.g. "placeitem") won't match the key (e.g. "place")
						// to retrieve the relevant record from one of the available
						// maps of record types.
						//
						// The following block is a fallback attempt at retrieving the
						// relevant record from a different map of record types, using the
						// first path component in the Services URI (e.g. "placeauthorities")
						// as the key.
						if(rec == null) {
                                                    if (in.has("summarylist")) {
                                                        JSONObject summaryList = in.getJSONObject("summarylist");
                                                        if (summaryList.has("uri")) {
                                                            String uri = summaryList.getString("uri");
                                                            if (Tools.notBlank(uri)) {
                                                                String recordtypekey = uri.split("/")[0];
                                                                if (Tools.notBlank(recordtypekey)) {
                                                                    rec = this.spec.getRecordByServicesUrl(recordtypekey);
                                                                }
                                                            }
                                                        }
                                                    }
						}
						String uiname;
						if(rec != null) {
							uiname = rec.getWebURL();
							if(rec.isType("authority")) {	// Need to add namespace for authorities
								String refName = null;
								if(in.has("refName")) {
									refName = in.getString("refName");
								} else if(in.has("summarylist")) {
									JSONObject summList = in.getJSONObject("summarylist");
									if(summList.has("refName")) {
										refName = summList.getString("refName");
									}
								}
								if(refName!=null) {
									RefName.AuthorityItem item = RefName.AuthorityItem.parse(refName); 
									in.put("namespace",item.getParentShortIdentifier());
								}
							}
						} else {
							uiname = rt;
						}
						in.put("recordtype", uiname);
						/*
						JSONObject entry=new JSONObject();
						entry.put("csid",in.getString("csid"));
						entry.put("recordtype",in.getString("sourceFieldType"));
						entry.put("sourceFieldName",field);
						entry.put("number",in.getString("sourceFieldName"));
						*/
						items.put(in);
					}
				}
				out.put(itemsKey, items);
				if(addPagination && refObjs.has("pagination")){
					out.put("pagination", refObjs.get("pagination"));
				}
			}
		}
		catch(JSONException ex){
			log.debug("JSONException"+ex.getLocalizedMessage());
			//wordlessly eat the errors at the moment as they might be permission errors
		} catch (ExistException e) {
			log.debug("ExistException"+e.getLocalizedMessage());
			//wordlessly eat the errors at the moment as they might be permission errors
		} catch (UnimplementedException e) {
			log.debug("UnimplementedException"+e.getLocalizedMessage());
			//wordlessly eat the errors at the moment as they might be permission errors
		} catch (UnderlyingStorageException e) {
			log.debug("UnderlyingStorageException"+e.getLocalizedMessage());
			//wordlessly eat the errors at the moment as they might be permission errors
		}
	}
	
	private JSONObject generateMiniRecord(Storage storage,String type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=storage.retrieveJSON(type+"/"+csid+"/view", new JSONObject());
		out.put("csid",csid);
		out.put("recordtype",type_to_url.get(type));
		return out;
	}

	private JSONObject generateRelationEntry(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		/* Retrieve entry */
		JSONObject in=storage.retrieveJSON("relations/main/"+csid, new JSONObject());
		String[] dstid=in.getString("dst").split("/");
		String type=in.getString("type");
		JSONObject mini=generateMiniRecord(storage,dstid[0],dstid[1]);
		mini.put("relationshiptype",type);
		mini.put("relid",in.getString("csid"));
		return mini;
	}
	
	private JSONObject createRelations(Storage storage,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject recordtypes=new JSONObject();
		JSONObject restrictions=new JSONObject();
		restrictions.put("src",base+"/"+csid);
		// XXX needs pagination support CSPACE-1819
		JSONObject data = storage.getPathsJSON("relations/main",restrictions);
		String[] relations = (String[]) data.get("listItems");
		for(String r : relations) {
			try {
				JSONObject relateitem = generateRelationEntry(storage,r);
				String type = relateitem.getString("recordtype");
				if(!recordtypes.has(type)){
					recordtypes.put(type, new JSONArray());
				}
				recordtypes.getJSONArray(type).put(relateitem);
			} catch(Exception e) {
				// Never mind.
			}
		}
		return recordtypes;
	}
	
	private JSONObject getHierarchy(Storage storage, JSONObject fields) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException{
		for(Relationship r: n.getRecord().getSpec().getAllRelations()){
			if(r.showSiblings()){
				//JSONObject temp = new JSONObject();
				//temp.put("_primary", true);
				JSONArray children = new JSONArray();
				//children.put(temp);
				fields.put(r.getSiblingParent(), children);
				if(fields.has(r.getID())){
					//String broadterm = fields.getString(r.getID());
					String child = r.getSiblingChild();
					if(fields.has(child)){
						String broader = fields.getString(child);
						
						JSONObject restrict=new JSONObject();
						restrict.put("dst",broader);	
						restrict.put("type","hasBroader");	
						JSONObject reldata = storage.getPathsJSON("relations/hierarchical",restrict);
						
						fields.remove(child);
						for(int i=0;i<reldata.getJSONObject("moredata").length();i++){

							String[] reld = (String[])reldata.get("listItems");
							String hcsid = reld[i];
							JSONObject mored = reldata.getJSONObject("moredata").getJSONObject(hcsid);
							//it's name is
							JSONObject siblings = new JSONObject();
							if(!fields.getString("csid").equals(mored.getString("subjectcsid"))){
								siblings.put(child,mored.getString("subjectrefname"));
								children.put(siblings);
							}
						}
					}
					fields.put(r.getSiblingParent(), children);
				}
			}
			//add empty array if necessary
			if(!fields.has(r.getID()) && r.mustExistInSpec()){
				if(r.getObject().equals("n")){
					JSONObject temp = new JSONObject();
					temp.put("_primary", true);
					JSONArray at = new JSONArray();
					at.put(temp);
					fields.put(r.getID(),at);
				}
				else{
					fields.put(r.getID(),"");
				}
			}
		}
		if(!fields.has("relatedTerms")){
			JSONObject temp = new JSONObject();
			temp.put("_primary", true);
			JSONArray at = new JSONArray();
			at.put(temp);
			fields.put("relatedTerms",at);
		}
		return fields;
	}
	
	/* Wrapper exists to decomplexify exceptions: also used inCreateUpdate, hence not private */
	JSONObject getJSON(Storage storage,String csid) throws UIException {
		return getJSON(storage, csid, new JSONObject());
	}

	private JSONObject getJSON(Storage storage,String csid, JSONObject restriction) throws UIException {
		JSONObject out=new JSONObject();
		try {
			String refPath = n.getRecord().getID()+"/"+n.getTitleRef()+"/";
			if(getInfoMode == GET_FULL_INFO || getInfoMode == GET_BASIC_INFO) {

				JSONObject fields=storage.retrieveJSON(refPath+csid, restriction);
				//add in equivalent hierarchy if relevant
				csid = fields.getString("csid");
				fields = getHierarchy(storage,fields);
				//fields.put("csid",csid);
				//JSONObject relations=createRelations(storage,csid);
				out.put("fields",fields);
				out.put("csid",csid);
				out.put("namespace",n.getWebURL());
			}

			if(getInfoMode == GET_FULL_INFO) {
				out.put("relations",new JSONArray());
				//out.put("relations",relations);
			}
			if(getInfoMode == GET_FULL_INFO) {
				JSONObject tusd = this.termsused.getTermsUsed(storage, refPath+csid, new JSONObject());
				out.put("termsUsed",tusd.getJSONArray("results"));
			}
			if(getInfoMode == GET_TERMS_USED_INFO) {
				JSONObject tusd = this.termsused.getTermsUsed(storage, refPath+csid, restriction);
				out.put("termsUsed",tusd);
			}
			if(getInfoMode == GET_FULL_INFO) {
				getRefObjs(storage,refPath+csid, out, "refobjs", false, restriction);
			} else if(getInfoMode == GET_REF_OBJS_INFO) {
				getRefObjs(storage,refPath+csid, out, "items", true, restriction);
			}
		} catch (ExistException e) {
			UIException uiexception =  new UIException(e.getMessage(),e);
			return uiexception.getJSON();
		} catch (UnimplementedException e) {
			UIException uiexception =  new UIException(e.getMessage(),e);
			return uiexception.getJSON();
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		} catch (JSONException e) {
			throw new UIException("Could not create JSON"+e,e);
		}
		return out;
	}
	
	public JSONObject getInstance(Storage storage) throws UIException {
		return getInstance(storage, new JSONObject());
	}

	private JSONObject getInstance(Storage storage, JSONObject restriction) throws UIException {
		JSONObject out=new JSONObject();

		try {
			String refPath = n.getRecord().getID() + "/" + n.getTitleRef() + "/";
			
			if(getInfoMode == GET_FULL_INFO || getInfoMode == GET_BASIC_INFO) {
				JSONObject fields=storage.retrieveJSON(refPath, restriction);
				
				if (fields.has("csid")) {
					String csid = fields.getString("csid");

					out.put("fields",fields);
					out.put("csid", csid);
				}
			}
		} catch (ExistException e) {
			UIException uiexception =  new UIException(e.getMessage(),e);
			return uiexception.getJSON();
		} catch (UnimplementedException e) {
			UIException uiexception =  new UIException(e.getMessage(),e);
			return uiexception.getJSON();
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			return uiexception.getJSON();
		} catch (JSONException e) {
			throw new UIException("Could not create JSON"+e,e);
		}
		return out;
	}

	private void store_get(Storage storage,UIRequest request,String path) throws UIException {
		// Get the data
		JSONObject restriction = new JSONObject();

		
		if(getInfoMode == GET_TERMS_USED_INFO || getInfoMode == GET_REF_OBJS_INFO) {
			Set<String> args = request.getAllRequestArgument();
			for (String restrict : args) {
				if (request.getRequestArgument(restrict) != null) {
					String value = request.getRequestArgument(restrict);
					if (restrict.equals(WebMethod.PAGE_SIZE_PARAM)
							|| restrict.equals(WebMethod.PAGE_NUM_PARAM)) {
						try {
							restriction.put(restrict, value);
						} catch (JSONException e) {
							log.warn("Problem with pagination request param (ignoring):"+e);
						}
					}
				}
			}
		}

		JSONObject outputJSON = getJSON(storage,path, restriction);
	
		// Write the requested JSON out
		request.sendJSONResponse(outputJSON);
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_get(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

}
