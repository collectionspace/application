/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericStorage  implements ContextualisedStorage {
	private static final Logger log=LoggerFactory.getLogger(GenericStorage.class);
	
	protected ServicesConnection conn;
	protected Record r;
	protected Map<String,String> view_good=new HashMap<String,String>();// map of servicenames of fields to descriptors
	protected Map<String,String> view_map=new HashMap<String,String>(); // map of csid to service name of field
	protected Set<String> xxx_view_deurn=new HashSet<String>();
	protected Map<String,List<String>> view_merge = new HashMap<String, List<String>>();

	
	public GenericStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		this.conn=conn;
		this.r=r;
	}

	protected void resetGlean(Record r){
		view_good=new HashMap<String,String>();
		view_map=new HashMap<String,String>();
		xxx_view_deurn=new HashSet<String>();
		view_merge = new HashMap<String, List<String>>();
		initializeGlean(r);
	}
	protected void resetGlean(Record r,Map<String,String> reset_good, Map<String,String>  reset_map, Set<String>  reset_deurn, Map<String,List<String>>  reset_merge){
		view_good=reset_good;
		view_map=reset_map;
		xxx_view_deurn=reset_deurn;
		view_merge = reset_merge;
		initializeGlean(r);
	}
	
	protected void initializeGlean(Record r){
		// Number
		if(r.getMiniNumber()!=null){
			view_good.put("number",r.getMiniNumber().getID());
			view_map.put(r.getMiniNumber().getServicesTag(),r.getMiniNumber().getID());
			if(r.getMiniNumber().hasMergeData()){
				view_merge.put("number",r.getMiniNumber().getAllMerge());				
			}
			if(r.getMiniNumber().hasAutocompleteInstance())
				xxx_view_deurn.add(r.getMiniNumber().getID());
		}
		// Summary
		if(r.getMiniSummary() !=null){
			view_good.put("summary",r.getMiniSummary().getID());
			view_map.put(r.getMiniSummary().getServicesTag(),r.getMiniSummary().getID());
			if(r.getMiniSummary().hasMergeData()){
				view_merge.put("summary",r.getMiniSummary().getAllMerge());				
			}
			if(r.getMiniSummary().hasAutocompleteInstance())
				xxx_view_deurn.add(r.getMiniSummary().getID());
		}
		//complex summary list
		if(r.getAllMiniDataSets().length>0){
			for(String setName : r.getAllMiniDataSets()){
				String prefix = setName;
				if(r.getMiniDataSetByName(prefix).length>0 && !prefix.equals("")){
					for(FieldSet fs : r.getMiniDataSetByName(prefix)){
						view_good.put(prefix+"_"+ fs.getID(),fs.getID());
						view_map.put(fs.getServicesTag(),fs.getID());
						if(fs instanceof Field) {
							Field f=(Field)fs;
							// Single field
							if(f.hasMergeData()){
								view_merge.put(prefix+"_"+f.getID(),f.getAllMerge());
								for(String fm : f.getAllMerge()){
									if(fm!=null){
										if(r.getRepeatField(fm).hasAutocompleteInstance()){
											xxx_view_deurn.add(f.getID());
										}
									}
								}
							}
							if(f.hasAutocompleteInstance()){
								xxx_view_deurn.add(f.getID());
							}
						}
						
					}
				}
			}
		}
	}

	/**
	 * Set the csids that were retrieved in the cache
	 * @param cache The cache itself
	 * @param path The path to the object on the service layer
	 * @param key The key for the node in the json file
	 * @param value The value for the node in the json file
	 */
	protected void setGleanedValue(CSPRequestCache cache,String path,String key,String value) {
		cache.setCached(getClass(),new String[]{"glean",path,key},value);
	}

	/**
	 * Get a value out of the cache
	 * @param {CSPRequestCache} cache The cache in which we store the csids
	 * @param {String} path The path to the record 
	 * @param {String} key The key to recreate the unique key to retrieve the cached value
	 * @return {String} The csid that was stored 
	 */
	protected String getGleanedValue(CSPRequestCache cache,String path,String key) {
		return (String)cache.getCached(getClass(),new String[]{"glean",path,key});
	}

	/**
	 * Convert an XML file into a JSON string
	 * @param {JSONObject} out The JSON string to which the XML has been converted
	 * @param {Document} in The XML document that has to be converted
	 * @throws JSONException
	 */
	protected void convertToJson(JSONObject out,Document in, String permlevel, String section) throws JSONException {
		XmlJsonConversion.convertToJson(out,r,in,permlevel,section);
	}
	/**
	 * Convert an XML file into a JSON string
	 * @param out
	 * @param in
	 * @param r
	 * @throws JSONException
	 */
	protected void convertToJson(JSONObject out,Document in, Record r, String permlevel, String section) throws JSONException {
		XmlJsonConversion.convertToJson(out,r,in,permlevel,section);
	}
	
	protected void getGleaned(){
		
	}


	protected String xxx_deurn(String in) throws UnderlyingStorageException {
		if(!in.startsWith("urn:"))
			return in;
		if(!in.endsWith("'"))
			return in;
		in=in.substring(0,in.length()-1);
		int pos=in.lastIndexOf("'");
		if(pos==-1)
			return in+"'";
		try {
			return URLDecoder.decode(in.substring(pos+1),"UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("No UTF8!");
		}
	}
	

	/**
	 * Get a mini view of the record
	 * Currently used for relatedObj relatedProc Find&Edit and Search
	 * this needs to be refactored as the 4 areas have different data set needs
	 * 
	 * @param cache
	 * @param creds
	 * @param filePath
	 * @param extra - used to define which subset of data to use expects related|terms|search|list
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
	public JSONObject miniViewRetrieveJSON(CSPRequestCache cache,CSPRequestCredentials creds,String filePath,String extra, String cachelistitem, Record thisr) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		if(cachelistitem==null){
			cachelistitem = thisr.getServicesURL()+"/"+filePath;
		}
		JSONObject out=new JSONObject();
		JSONObject summarylist=new JSONObject();
		String summarylistname = "summarylist_";
		if(!extra.equals("")){
			summarylistname = extra+"_";
		}
		Set<String> to_get=new HashSet<String>(view_good.keySet());
		// Try to fullfil from gleaned info
		//gleaned is info that everytime we read a record we cache certain parts of it
		for(String fieldname : view_good.keySet()) {
			//only get the info that is needed
			String name = fieldname;
			if(!name.startsWith(summarylistname) && !name.equals("summary") && !name.equals("number")){
				to_get.remove(fieldname);
				continue;
			}
			
			String gleaned = null;
			String good = view_good.get(fieldname);
			if(view_merge.containsKey(fieldname)){
				List<String> mergeids = view_merge.get(fieldname);
				for(String id : mergeids){
					if(id == null)
						continue;
					//iterate for merged ids
					gleaned=getGleanedValue(cache,cachelistitem,id);
					if(gleaned!=null && gleaned !=""){
						//if find value stop
						break;
					}
				}
			}
			else{
				gleaned=getGleanedValue(cache,cachelistitem,good);
			}
			
			if(gleaned==null)
				continue;
			if(xxx_view_deurn.contains(good))
				gleaned=xxx_deurn(gleaned);
			
			
			if(name.startsWith(summarylistname)){
				name = name.substring(summarylistname.length());
				summarylist.put(name, gleaned);
			}
			else{
				out.put(fieldname,gleaned);
			}
			to_get.remove(fieldname);
		}
		// Do a full request only if values in list of fields returned != list from cspace-config
		if(to_get.size()>0) {
			JSONObject data=simpleRetrieveJSON(creds,cache,filePath,thisr);
			for(String fieldname : to_get) {
				String good = view_good.get(fieldname);
				String value = null;
				if(view_merge.containsKey(fieldname)){
					List<String> mergeids = view_merge.get(fieldname);
					for(String id : mergeids){
						if(id == null)
							continue;
						value = JSONUtils.checkKey(data, id);
						//iterate for merged ids
						if(value!=null && value !=""){
							//if find value stop
							break;
						}
					}
				}
				else{
					value = JSONUtils.checkKey(data, good);
				}
				//this might work with repeat objects
				if(value != null){
					String vkey=fieldname;
					if(xxx_view_deurn.contains(good))
						value=xxx_deurn(value);
				
					if(vkey.startsWith(summarylistname)){
						String name = vkey.substring(summarylistname.length());
						summarylist.put(name, value);
					}
					else{
						out.put(vkey,value);
					}
				}
				else{
					String vkey=fieldname;
					if(vkey.startsWith(summarylistname)){
						String name = vkey.substring(summarylistname.length());
						summarylist.put(name, "");
					}
					else{
						out.put(vkey,"");						
					}
				}
			}
		}
		if(summarylist.length()>0){
			out.put("summarylist", summarylist);
		}
		return out;
	}
	

	/**
	 * return data just as the service layer gives it to the App layer
	 * no extra columns required
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		out = simpleRetrieveJSON(creds,cache,filePath,r);
		return out;
	}
	
	/**
	 * return data just as the service layer gives it to the App layer
	 * no extra columns required
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param Record
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds,CSPRequestCache cache,String filePath, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {

		JSONObject out=new JSONObject();
		out = simpleRetrieveJSON(creds,cache,filePath,thisr.getServicesURL()+"/", thisr);
		return out;
	}

	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds,CSPRequestCache cache,String filePath, String servicesurl, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out=new JSONObject();
			if(thisr.isMultipart()){
				ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,servicesurl+filePath,null,creds,cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),filePath);
				
				for(String section : thisr.getServicesRecordPaths()) {
					String path=thisr.getServicesRecordPath(section);
					String[] parts=path.split(":",2);
					if(doc.getDocument(parts[0]) != null ){
						convertToJson(out,doc.getDocument(parts[0]), thisr, "GET",section);
					}
				}
			}else{
				ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, servicesurl+filePath,null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),filePath);
				convertToJson(out,doc.getDocument(), thisr, "GET", "common");
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}
	/**
	 * Construct different data sets for different view needs
	 * 
	 * 
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param view - view|refs used to define what type of data is needed
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException 
	 * @throws ConnectionException 
	 */
	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view, String extra, JSONObject restrictions) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		if(view.equals("view")){
			//get a view of a specific item e.g. for search results
			return miniViewRetrieveJSON(cache,creds,filePath, extra, null,r);
		}
		else if("refs".equals(view)){
			String path = r.getServicesURL()+"/"+filePath+"/authorityrefs";
			return refViewRetrieveJSON(storage,creds,cache,path,restrictions);
		}
		else if("refObjs".equals(view)){
			String path = r.getServicesURL()+"/"+filePath+"/refObjs";
			return refObjViewRetrieveJSON(storage,creds,cache,path);
		}
		else
			return new JSONObject();
	}

	// XXX support URNs for reference
	protected JSONObject miniForURI(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String refname,String uri, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		return storage.retrieveJSON(storage,creds,cache,"direct/urn/"+uri+"/"+refname, restrictions);
	}


	/**
	 * get data needed for terms Used block of a record
	 * @param creds
	 * @param cache
	 * @param path
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException 
	 */
	public JSONObject refViewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String path, JSONObject restrictions) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		try {
			JSONObject out=new JSONObject();
			//not all the records need a reference, look in default.xml for which that don't
			if(r.hasTermsUsed()){
				path =  getRestrictedPath(path, restrictions);
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
				String data2 = all.getDocument().asXML();
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200",all.getStatus(),path);
				Document list=all.getDocument();
				for(Object node : list.selectNodes("authority-ref-list/authority-ref-item")) {
					if(!(node instanceof Element))
						continue;
					if(((Element) node).hasContent()){

						String key=((Element)node).selectSingleNode("sourceField").getText();
						String uri=((Element)node).selectSingleNode("uri").getText();
						String refname=((Element)node).selectSingleNode("refName").getText();

						String fieldName = key;
						if(key.split(":").length>1){
							fieldName = key.split(":")[1];
						}
						
						Field fieldinstance= null;
						if(r.getRepeatField(fieldName) instanceof Repeat){
							Repeat rp = (Repeat)r.getRepeatField(fieldName);
							for(FieldSet a : rp.getChildren("GET")){
								if(a instanceof Field && a.hasAutocompleteInstance()){
									fieldinstance = (Field)a;
								}
							}
						}
						else{
							fieldinstance = (Field)r.getRepeatField(fieldName);
						}
						
						if(fieldinstance != null){
						
							if(uri!=null && uri.startsWith("/"))
								uri=uri.substring(1);
							JSONObject data=miniForURI(storage,creds,cache,refname,uri,restrictions);
							data.put("sourceFieldselector", fieldinstance.getSelector());
							data.put("sourceFieldName", fieldName);
							data.put("sourceFieldType", r.getID());
							if(out.has(key)){
								JSONArray temp = out.getJSONArray(key).put(data);
								out.put(key,temp);
							}
							else{
								JSONArray temp = new JSONArray();
								temp.put(data);
								out.put(key,temp);
							}
						}
					}
				}
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection problem"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} 
	}

	/**
	 * get data needed for list of objects related to a termUsed
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param path
	 * @return
	 * @throws ExistException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 * @throws UnimplementedException 
	 */
	public JSONObject refObjViewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String path) throws ExistException, UnderlyingStorageException, JSONException, UnimplementedException {

		JSONObject out=new JSONObject();
		try{

			Map<String,String> reset_good=new HashMap<String,String>();// map of servicenames of fields to descriptors
			Map<String,String> reset_map=new HashMap<String,String>(); // map of csid to service name of field
			Set<String> reset_deurn=new HashSet<String>();
			Map<String,List<String>> reset_merge = new HashMap<String, List<String>>();
			
			if(r.hasRefObjUsed()){
				//XXX need a way to append the data needed from the field whcih we don't know until after we have got the information...
				reset_map.put("docType", "docType");
				reset_map.put("docId", "docId");
				reset_map.put("docNumber", "docNumber");
				reset_map.put("sourceField", "sourceField");
				reset_map.put("uri", "uri");
				reset_good.put("terms_docType", "docType");
				reset_good.put("terms_docId", "docId");
				reset_good.put("terms_docNumber", "docNumber");
				reset_good.put("terms_sourceField", "sourceField");

				view_good = reset_good;
				view_map = reset_map;
				xxx_view_deurn = reset_deurn;
				view_merge = reset_merge;
				
				String nodeName = "authority-ref-doc-list/authority-ref-doc-item";
				JSONObject data = getListView(storage,creds,cache,path,nodeName,"/authority-ref-doc-list/authority-ref-doc-item","uri", true);

				reset_good = view_good;
				reset_map = view_map;
				reset_deurn = xxx_view_deurn;
				reset_merge = view_merge;
				
				String[] filepaths = (String[]) data.get("listItems");
				for(String uri : filepaths) {
					String filePath = uri;
					if(filePath!=null && filePath.startsWith("/"))
						filePath=filePath.substring(1);
					
					String[] parts=filePath.split("/");
					String recordurl = parts[0];
					Record thisr = r.getSpec().getRecordByServicesUrl(recordurl);
					resetGlean(thisr,reset_good,reset_map,reset_deurn,reset_merge);// what glean info required for this one..
					String csid = parts[parts.length-1];
					JSONObject dataitem =  miniViewRetrieveJSON(cache,creds,csid, "terms", r.getServicesURL()+"/"+uri, thisr);
					dataitem.getJSONObject("summarylist").put("uri",filePath);
					
					String key = dataitem.getJSONObject("summarylist").getString("sourceField");
					String fieldName = "unknown";
					String fieldSelector = "unknown";
					if(key.contains(":")){
						fieldName = key.split(":")[1];
						//XXX fixCSPACE-2909 would be nice if they gave us the actual field rather than the parent
						//XXX CSPACE-2586
						while(thisr.getRepeatField(fieldName) instanceof Repeat || thisr.getRepeatField(fieldName) instanceof Group ){
							fieldName = ((Repeat)thisr.getRepeatField(fieldName)).getChildren("GET")[0].getID();
						}
						Field fieldinstance = (Field)thisr.getRepeatField(fieldName);
						fieldSelector = fieldinstance.getSelector();
					}

					dataitem.put("csid", csid);
					dataitem.put("sourceFieldselector", fieldSelector);
					dataitem.put("sourceFieldName", fieldName);
					dataitem.put("sourceFieldType", dataitem.getJSONObject("summarylist").getString("docType"));
					
					out.put(key,dataitem);
				}
			/*	
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
				String test = all.getDocument().asXML();
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200");
				Document list=all.getDocument();
				for(Object node : list.selectNodes("authority-ref-doc-list/authority-ref-doc-item")) {
					if(!(node instanceof Element))
						continue;
					String key=((Element)node).selectSingleNode("sourceField").getText();
					String uri=((Element)node).selectSingleNode("uri").getText();
					String docid=((Element)node).selectSingleNode("docId").getText();
					String doctype=((Element)node).selectSingleNode("docType").getText();
					String fieldName = key.split(":")[1];
					//Field fieldinstance = (Field)r.getRepeatField(fieldName);
					
					if(uri!=null && uri.startsWith("/"))
						uri=uri.substring(1);
					JSONObject data = new JSONObject();//=miniForURI(storage,creds,cache,refname,uri);
					data.put("csid", docid);
//					data.put("sourceFieldselector", fieldinstance.getSelector());
					data.put("sourceFieldName", fieldName);
					data.put("sourceFieldType", doctype);
					out.put(key,data);
				}
				*/
			}
			return out;
		} catch (ConnectionException e) {
			log.error("failed to retrieve refObjs for "+path);
			JSONObject dataitem = new JSONObject();
			dataitem.put("csid", "");
			dataitem.put("sourceFieldselector", "Functionality Failed");
			dataitem.put("sourceFieldName", "Functionality Failed");
			dataitem.put("sourceFieldType", "Functionality Failed");
			dataitem.put("message", e.getMessage());
			
			out.put("Functionality Failed",dataitem);
			//return out;
			throw new UnderlyingStorageException("Connection problem"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject,Record thisr, String serviceurl)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Map<String,Document> parts=new HashMap<String,Document>();
			Document doc = null;
			for(String section : thisr.getServicesRecordPaths()) {
				String path=thisr.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				doc=XmlJsonConversion.convertToXml(thisr,jsonObject,section,"PUT");
				if(doc !=null){
					parts.put(record_path[0],doc);
				}
			}
			int status = 0;
			if(thisr.isMultipart()){
				ReturnedMultipartDocument docm = conn.getMultipartXMLDocument(RequestMethod.PUT,serviceurl+filePath,parts,creds,cache);
				status = docm.getStatus();
			}
			else{ 
				ReturnedDocument docm = conn.getXMLDocument(RequestMethod.PUT, serviceurl+filePath, doc, creds, cache);
				status = docm.getStatus();
			}
			
			//if(status==404)
			//	throw new ExistException("Not found: "+serviceurl+filePath);
			if(status>299 || status<200)
				throw new UnderlyingStorageException("Bad response ",status,serviceurl+filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}
	
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject,Record thisr)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		updateJSON( root, creds, cache, filePath,  jsonObject, thisr, thisr.getServicesURL()+"/");
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		updateJSON( root, creds, cache, filePath,  jsonObject, r);
	}
	
	
	/**
	 * Convert the JSON from the UI Layer into XML for the Service layer while using the XML structure from default.xml
	 * Send the XML through to the Service Layer to store it in the database
	 * The Service Layer returns a url to the object we just stored.
	 * @param {ContextualisedStorage} root 
	 * @param {CSPRequestCredentials} creds
	 * @param {CSPRequestCache} cache
	 * @param {String} filePath part of the path to the Service URL (containing the type of object)
	 * @param {JSONObject} jsonObject The JSON string coming in from the UI Layer, containing the object to be stored
	 * @return {String} csid The id of the object in the database
	 */
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {

			ReturnedURL url = null;
			Document doc = null;
			//used by userroles and permroles as they have complex urls
			if(r.hasPrimaryField()){
				//XXX test if works: need to delete first before create/update
			//	deleteJSON(root,creds,cache,filePath);

				for(String section : r.getServicesRecordPaths()) {
					doc=XmlJsonConversion.convertToXml(r,jsonObject,section,"POST");
					String path = r.getServicesURL();
					path = path.replace("*", getSubCsid(jsonObject,r.getPrimaryField()));

					deleteJSON(root,creds,cache,path);
					url = conn.getURL(RequestMethod.POST, path, doc, creds, cache);	
				}
			}
			else{
				url = autoCreateSub(creds, cache, jsonObject, doc);
			}
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException(e.getMessage(),e.getStatus(), e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}

	protected ReturnedURL autoCreateSub(CSPRequestCredentials creds,
			CSPRequestCache cache, JSONObject jsonObject, Document doc)
			throws JSONException, UnderlyingStorageException,
			ConnectionException {
		ReturnedURL url;
		Map<String,Document> parts=new HashMap<String,Document>();
		for(String section : r.getServicesRecordPaths()) {
			String path=r.getServicesRecordPath(section);
			String[] record_path=path.split(":",2);
			doc=XmlJsonConversion.convertToXml(r,jsonObject,section,"POST");
			if(doc!=null){
				parts.put(record_path[0],doc);
			}
		}
		//some records are accepted as multipart in the service layers, others arent, that's why we split up here
		if(r.isMultipart())
			url = conn.getMultipartURL(RequestMethod.POST,r.getServicesURL()+"/",parts,creds,cache);
		else
			url = conn.getURL(RequestMethod.POST, r.getServicesURL()+"/", doc, creds, cache);
		if(url.getStatus()>299 || url.getStatus()<200)
			throw new UnderlyingStorageException("Bad response ",url.getStatus(),r.getServicesURL()+"/");
		return url;
	}
	
	/**
	 * Gets the csid from an role or account out of the json authorization
	 * @param data
	 * @param primaryField
	 * @return
	 * @throws JSONException
	 */
	protected String getSubCsid(JSONObject data, String primaryField) throws JSONException{
		String[] path = primaryField.split("/");
		JSONObject temp = data;
		int finalnum = path.length - 1;
		for(int i=0;i<finalnum; i++){
			if(temp.has(path[i])){
				temp = temp.getJSONObject(path[i]);
			}
		}
		String csid = temp.getString(path[finalnum]);
		return csid;
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot post to full path");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, String serviceurl) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			int status=conn.getNone(RequestMethod.DELETE,serviceurl+filePath,null,creds,cache);
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception",status,serviceurl+filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}		
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		String serviceurl = thisr.getServicesURL()+"/";
		deleteJSON(root,creds,cache,filePath,serviceurl);
	}
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		deleteJSON(root,creds,cache,filePath,r);
	}

	@Override
	public String[] getPaths(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache,
			String rootPath, JSONObject restrictions) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		// TODO Auto-generated method stub
		return null;
	}

	private String getRestrictedPath(String basepath, JSONObject restrictions) throws UnsupportedEncodingException, JSONException{

		String postfix = "?";
		String prefix=null;
		Boolean queryadded = false;
		if(restrictions!=null){
			if(restrictions.has("keywords")) {
				/* Keyword search */
				String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
				postfix += "kw="+data+"&";
			} 
			if(restrictions.has("pageSize")){
				postfix += "pgSz="+restrictions.getString("pageSize")+"&";
			}
			if(restrictions.has("pageNum")){
				postfix += "pgNum="+restrictions.getString("pageNum")+"&";
			}
			if(restrictions.has("queryTerm")){
				String queryString = prefix;
				if(restrictions.has("queryString")){
					queryString=restrictions.getString("queryString");
				}
				postfix+=restrictions.getString("queryTerm")+"="+URLEncoder.encode(queryString,"UTF8")+"&";
				queryadded = true;
			}
		}
		//if(prefix!=null && !queryadded){
		//	postfix+="pt="+URLEncoder.encode(prefix,"UTF8")+"&";
		//}
		postfix = postfix.substring(0, postfix.length()-1);
		if(postfix.length() == 0){postfix +="/";}
		
		String path = basepath+postfix;
		return path;
	}

	/**
	 * Gets a list of csids of a certain type of record together with the pagination info
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String path = getRestrictedPath(r.getServicesURL(), restrictions);
			
			String node = "/"+r.getServicesListPath().split("/")[0]+"/*";
			JSONObject data = getListView(root,creds,cache,path,node,"/"+r.getServicesListPath(),"csid",false);
			
			return data;
			
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}


	@SuppressWarnings("unchecked")
	protected JSONObject getListView(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid) throws ConnectionException, JSONException {
		JSONObject out = new JSONObject();
		JSONObject pagination = new JSONObject();
		Document list=null;
		List<String> listitems=new ArrayList<String>();
		ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
		if(all.getStatus()!=200){
			//throw new StatusException(all.getStatus(),path,"Bad request during identifier cache map update: status not 200");
			throw new ConnectionException("Bad request during identifier cache map update: status not 200"+Integer.toString(all.getStatus()),all.getStatus(),path);
		}
		list=all.getDocument();
		
		List<Node> nodes=list.selectNodes(nodeName);
		if(matchlistitem.equals("roles_list/*") || matchlistitem.equals("permissions_list/*")){
			//XXX hack to deal with roles being inconsistent
			//XXX CSPACE-1887 workaround
			for(Node node : nodes) {
				if(node.matches(matchlistitem)){
					String csid = node.valueOf( "@csid" );
					listitems.add(csid);
				}
				else{
					pagination.put(node.getName(), node.getText());
				}
			}
		}
		else{
			for(Node node : nodes) {
				if(node.matches(matchlistitem)){
					List<Node> fields=node.selectNodes("*");
					String csid="";
					if(node.selectSingleNode(csidfield)!=null){
						csid=node.selectSingleNode(csidfield).getText();
					}
					for(Node field : fields) {
						if(csidfield.equals(field.getName())) {
							if(!fullcsid){
								int idx=csid.lastIndexOf("/");
								if(idx!=-1)
									csid=csid.substring(idx+1);
							}
							listitems.add(csid);
						} else {
							String json_name=view_map.get(field.getName());
							if(json_name!=null) {
								String value=field.getText();
								// XXX hack to cope with multi values		
								if(value==null || "".equals(value)) {
									List<Node> inners=field.selectNodes("*");
									for(Node n : inners) {
										value+=n.getText();
									}
								}
								setGleanedValue(cache,r.getServicesURL()+"/"+csid,json_name,value);
							}
						}
					}
					/* this hopefully will reduce fan out - needs more testing */
					if(list.selectSingleNode(r.getServicesFieldsPath())!=null){
						String myfields = list.selectSingleNode(r.getServicesFieldsPath()).getText();
						String[] allfields = myfields.split("\\|");
						for(String s : allfields){
							String gleaned = getGleanedValue(cache,r.getServicesURL()+"/"+csid,s);
							if(gleaned==null){
								setGleanedValue(cache,r.getServicesURL()+"/"+csid,s,"");
							}
						}
					}
				}
				else{
					pagination.put(node.getName(), node.getText());
				}
			}
		}
		out.put("pagination", pagination);
		out.put("listItems", listitems.toArray(new String[0]));
		return out;
		
		
	}
	
	/**
	 * Get data about objects. If filePath is made of 2 elements then this is a specific view of the object
	 * e.g. for related objs, search etcn and therefore will require a different dataset returned
	 * @throws  
	 */
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=filePath.split("/");
			if(parts.length>=2) {
				String extra = "";
				if(parts.length==3){
					extra = parts[2];
				}
				return viewRetrieveJSON(root,creds,cache,parts[0],parts[1],extra, restrictions);
			} else
				return simpleRetrieveJSON(creds,cache,filePath);
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		} catch (UnsupportedEncodingException x) {
			throw new UnderlyingStorageException("Error UnsupportedEncodingException JSON",x);
		}
	}


}
