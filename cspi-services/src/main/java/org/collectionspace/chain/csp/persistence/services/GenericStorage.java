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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * Class exists to: house all the generic aspects to how we call and deal with data form the service layer
 * Extended by :RecordStorage,  BlobStorage, ConfiguredVocabStorage etc
 * implements; abstract Contextuali
 * Future development: bring Vocab Storage more into the fold with other storage methodologies
 * @author csm22
 *
 */
public class GenericStorage  implements ContextualisedStorage {

	private static final Logger log=LoggerFactory.getLogger(GenericStorage.class);
	
	protected ServicesConnection conn;
	protected Record r;
	protected Map<String,String> view_good=new HashMap<String,String>();// map of servicenames of fields to descriptors
	protected Map<String,String> view_map=new HashMap<String,String>(); // map of csid to service name of field
	protected Set<String> xxx_view_deurn=new HashSet<String>();
	protected Map<String,List<String>> view_merge = new HashMap<String, List<String>>();

	/**
	 * Constructor
	 * @param r
	 * @param conn
	 * @throws DocumentException
	 * @throws IOException
	 */
	public GenericStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		this.conn=conn;
		this.r=r;
	}

	/**
	 * re intialises empty glean array
	 * was a quick fix that needs to be looked at again
	 * could be optimised better
	 * @param r
	 */
	protected void resetGlean(Record r){
		view_good=new HashMap<String,String>();
		view_map=new HashMap<String,String>();
		xxx_view_deurn=new HashSet<String>();
		view_merge = new HashMap<String, List<String>>();
		initializeGlean(r);
	}
	/**
	 * reinitialises filled glean array
	 * needed because things changes that we didn't initially anticipate
	 * was a quick fix that needs to be looked at again
	 * could be optimised better
	 * @param r
	 * @param reset_good
	 * @param reset_map
	 * @param reset_deurn
	 * @param reset_merge
	 * @param init
	 */
	protected void resetGlean(Record r,Map<String,String> reset_good, Map<String,String>  reset_map, Set<String>  reset_deurn, Map<String,List<String>>  reset_merge, Boolean init){
		view_good=reset_good;
		view_map=reset_map;
		xxx_view_deurn=reset_deurn;
		view_merge = reset_merge;
		if(init){
			initializeGlean(r);
		}
	}
	
	/**
	 * initialise the info for the glean array
	 * view_good: map of servicenames of fields to descriptors
	 * view_map: map of csid to service name of field
	 * view_merge: map of fields needed if one field to the UI is really multiple fields in the services
	 * 
	 * view_good keys are prefixed by the values found in the mini tag in the config file 
	 * to help simplify deciding what to find and reduce irrelevant fan out, 
	 * @param r
	 */
	protected void initializeGlean(Record r){
		// Number
		if(r.getMiniNumber()!=null){
			view_good.put("number",r.getMiniNumber().getID());
			view_map.put(r.getMiniNumber().getServicesTag(),r.getMiniNumber().getID());
			if(r.getMiniNumber().hasMergeData()){
				view_merge.put("number",r.getMiniNumber().getAllMerge());				
			}
			if(r.getMiniNumber().hasAutocompleteInstance()
					// Cannot know for searchall records, so just de-urn the 
					// number and summary.
					// Why not just de-urn them all? It is quiet about ignoring
					// values that do not start with urn:
					|| r.isType("searchall"))
				xxx_view_deurn.add(r.getMiniNumber().getID());
		}
		// Summary
		if(r.getMiniSummary() !=null){
			view_good.put("summary",r.getMiniSummary().getID());
			view_map.put(r.getMiniSummary().getServicesTag(),r.getMiniSummary().getID());
			if(r.getMiniSummary().hasMergeData()){
				view_merge.put("summary",r.getMiniSummary().getAllMerge());				
			}
			if(r.getMiniSummary().hasAutocompleteInstance()
					// Cannot know for searchall records, so just de-urn the 
					// number and summary.
					// Why not just de-urn them all? It is quiet about ignoring
					// values that do not start with urn:
					|| r.isType("searchall"))
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
										if(r.getFieldFullList(fm).hasAutocompleteInstance()){
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
		if(!path.startsWith("/")){
			path = "/"+path;
		}
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
		if(!path.startsWith("/")){
			path = "/"+path;
		}
		return (String)cache.getCached(getClass(),new String[]{"glean",path,key});
	}

	/**
	 * Convert an XML file into a JSON string
	 * @param {JSONObject} out The JSON string to which the XML has been converted
	 * @param {Document} in The XML document that has to be converted
	 * @throws JSONException
	 */
	protected void convertToJson(JSONObject out,Document in, Record thisr, String permlevel, String section,String csid,String ims_base) throws JSONException {
		XmlJsonConversion.convertToJson(out,thisr,in,permlevel,section,csid,ims_base);
	}
	/**
	 * Convert an XML file into a JSON string
	 * @param out
	 * @param in
	 * @param r
	 * @throws JSONException
	 */
	protected void convertToJson(JSONObject out,Document in, Record thisr, String permlevel, String section, String csid) throws JSONException {
		XmlJsonConversion.convertToJson(out,thisr,in,permlevel,section,csid,conn.getIMSBase());
	}
	
	protected void getGleaned(){
		
	}


	/**
	 * get de urned value of a field
	 * this function is called xxx_ as I don't believe that is is best that this happens in the App layer
	 * @param in
	 * @return
	 * @throws UnderlyingStorageException
	 */
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
			cachelistitem = "/"+thisr.getServicesURL()+"/"+filePath;
		}

		if(!cachelistitem.startsWith("/")){
			cachelistitem = "/"+cachelistitem;
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

		//check summary and number against docNumber docName
		if(to_get.contains("summary")){
			String gleaned=getGleanedValue(cache,cachelistitem,"docName");
			if(gleaned!=null){
			String good = view_good.get("summary");
			if(xxx_view_deurn.contains(good))
				gleaned=xxx_deurn(gleaned);
				out.put("summary",gleaned);
				to_get.remove("summary");
			}
		}
		//check summary and number against docNumber docName
		if(to_get.contains("number")){
			String gleaned=getGleanedValue(cache,cachelistitem,"docNumber");
			if(gleaned!=null){
			String good = view_good.get("number");
			if(xxx_view_deurn.contains(good))
				gleaned=xxx_deurn(gleaned);
				out.put("number",gleaned);
				to_get.remove("number");
			}
		}
		
		// Do a full request only if values in list of fields returned != list from cspace-config
		if(to_get.size()>0) {
			JSONObject data=simpleRetrieveJSON(creds,cache,null,cachelistitem,thisr);
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

	/**
	 * return data just as the service layer gives it to the App layer
	 * no extra columns required
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param servicesurl
	 * @param thisr
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds, CSPRequestCache cache, String filePath, String servicesurl, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		String csid="";
		if(filePath ==null){filePath="";}
		String[] path_parts = filePath.split("/");
		if(path_parts.length>1)
			csid = path_parts[1];
		else
			csid = filePath;
		
		JSONObject out=new JSONObject();
		try {
			String softpath = filePath;
			if(thisr.hasSoftDeleteMethod()){
				softpath = softpath(filePath);
			}
			if(thisr.hasHierarchyUsed("screen")){
				softpath = hierarchicalpath(softpath);
			}
			
			if(thisr.isMultipart()){
				ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,servicesurl+softpath,null,creds,cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),softpath);
				
				for(String section : thisr.getServicesRecordPaths()) {
					String path=thisr.getServicesRecordPath(section);
					String[] parts=path.split(":",2);
					if(doc.getDocument(parts[0]) != null ){
						convertToJson(out,doc.getDocument(parts[0]), thisr, "GET",section , csid);
					}
				}
			}
			else{
				ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, servicesurl+softpath,null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),softpath);
				convertToJson(out,doc.getDocument(), thisr, "GET", "common",csid);
			}

		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
		
		/*
		 * Get data for any sub records that are part of this record e.g. contact in person, blob in media
		 */
		try{
			for(FieldSet fs : thisr.getAllSubRecords("GET")){
				Boolean validator = true;
				Record sr = fs.usesRecordId();
				if(fs.usesRecordValidator()!= null){
					validator = false;
					if(out.has(fs.usesRecordValidator())){
						String test = out.getString(fs.usesRecordValidator());
						if(test!=null && !test.equals("")){
							validator = true;
						}
					}
				}
				if(validator){
					String getPath = servicesurl+filePath + "/" + sr.getServicesURL();
					if(null !=fs.getServicesUrl()){
						getPath = fs.getServicesUrl();
					}
					if(fs.getWithCSID()!=null) {
						getPath = getPath + "/" + out.getString(fs.getWithCSID());
					}
					
					//seems to work for media blob
					//need to get update and delete working? tho not going to be used for media as handling blob seperately
					if(fs instanceof Group){
						JSONObject outer = simpleRetrieveJSON(creds,cache,getPath,"",sr);
						JSONArray group = new JSONArray();
						group.put(outer);
						out.put(fs.getID(), group);
					}
					if(fs instanceof Repeat){
						//NEED TO GET A LIST OF ALL THE THINGS
						JSONArray repeat = new JSONArray();
						String path = getPath;
						String node = "/"+sr.getServicesListPath().split("/")[0]+"/*";
						
						while(!path.equals("")){
							JSONObject data = getListView(creds,cache,path,node,"/"+sr.getServicesListPath(),"csid",false,r);
							if(data.has("listItems")){
								String[] results = (String[]) data.get("listItems");

								for(String result : results) {
									JSONObject rout=simpleRetrieveJSON( creds, cache,getPath+"/"+result, "", sr);
									rout.put("_subrecordcsid", result);//add in csid so I can do update with a modicum of confidence
									repeat.put(rout);
								}
							}
							if(data.has("pagination")){
								Integer ps = Integer.valueOf(data.getJSONObject("pagination").getString("pageSize"));
								Integer pn = Integer.valueOf(data.getJSONObject("pagination").getString("pageNum"));
								Integer ti = Integer.valueOf(data.getJSONObject("pagination").getString("totalItems"));
								if(ti > (ps * (pn +1))){
									JSONObject restrictions = new JSONObject();
									restrictions.put("pageSize", Integer.toString(ps));
									restrictions.put("pageNum", Integer.toString(pn + 1));

									path = getRestrictedPath(getPath, restrictions, sr.getServicesSearchKeyword(), "", false, "");
									//need more values
								}
								else{
									path = "";
								}
							}
						}
						//group.put(outer);
						out.put(fs.getID(), repeat);
					}
				}
			}
		}
		catch (Exception e) {
			//ignore exceptions for sub records at the moment - make it more intelligent later
			//throw new UnderlyingStorageException("Service layer exception",e);
		}
		return out;
	}
	/**
	 * Construct different data sets for different view needs
	 * view returns mini view of item
	 * refs does services authorityref call
	 * refobjs does services refObjs call
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param view - view|refs|refObjs used to define what type of data is needed
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException 
	 * @throws ConnectionException 
	 */
	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view, String extra, JSONObject restrictions, String servicepath) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		if(view.equals("view")){
			//get a view of a specific item e.g. for search results
			JSONObject temp = miniViewRetrieveJSON(cache,creds,filePath, extra, servicepath,r);
			return miniViewAbstract(storage, creds, cache, temp, servicepath, filePath);
		}
		else if("refs".equals(view)){
			String path = servicepath+"/authorityrefs";
			return refViewRetrieveJSON(storage,creds,cache,path,restrictions);
		}
		else if("refObjs".equals(view)){
			String path = servicepath+"/refObjs";
			return refObjViewRetrieveJSON(storage,creds,cache,path);
		}
		else
			return new JSONObject();
	}
	/**
	 * only exists currently so it can be extended in vocabStorage
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param data
	 * @param servicepath
	 * @param filePath
	 * @return
	 * @throws UnderlyingStorageException
	 */
	protected JSONObject miniViewAbstract(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,JSONObject data, String servicepath, String filePath) throws UnderlyingStorageException{
		return data;
	}
	/**
	 * finds path based on record type and calls viewRetrieveJSON with more parameters
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param view
	 * @param extra
	 * @param restrictions
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view, String extra, JSONObject restrictions) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		String servicepath =  r.getServicesURL()+"/"+filePath;
		return viewRetrieveJSON(storage,creds,cache,filePath,view,extra,restrictions,servicepath);
	}

	/**
	 * gets the authority item based on the refname rather than the csid
	 * XXX need to support URNs for reference
	 * @param storage
	 * @param creds
	 * @param cache
	 * @param refname
	 * @param uri
	 * @param restrictions
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 * @throws JSONException
	 */
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
			JSONObject pagination=new JSONObject();
			JSONObject listitems = new JSONObject();
			//not all the records need a reference, look in cspace-config.xml for which that don't
			if(r.hasTermsUsed()){
				path =  getRestrictedPath(path, restrictions,"kw", "", false, "");
				if(r.hasSoftDeleteMethod()){//XXX completely not the right thinsg to do but a good enough hack
					path = softpath(path);
				}
				if(r.hasHierarchyUsed("screen")){
					path = hierarchicalpath(path);
				}
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200",all.getStatus(),path);
				Document list=all.getDocument();

				//assumes consistency in service layer payloads - possible could configure this rather than hard code?
				List<Node> nodes=list.selectNodes("authority-ref-list/*");
				for(Node node : nodes) {
					if(node.getName().equals("authority-ref-item")){
						if(!(node instanceof Element))
							continue;
						if(((Element) node).hasContent()){
	
							String key=((Element)node).selectSingleNode("sourceField").getText();
							String refname=((Element)node).selectSingleNode("refName").getText();
							String itemDisplayName=((Element)node).selectSingleNode("itemDisplayName").getText();
							String uri = "";
							if(null!=((Element)node).selectSingleNode("uri")){ //seems to be missing sometimes
								uri=((Element)node).selectSingleNode("uri").getText();
							}
							
							String fieldName = key;
							if(key.split(":").length>1){
								fieldName = key.split(":")[1];
							}
							
							Field fieldinstance= null;
							if(r.getFieldFullList(fieldName) instanceof Repeat){
								Repeat rp = (Repeat)r.getFieldFullList(fieldName);
								for(FieldSet a : rp.getChildren("GET")){
									if(a instanceof Field && a.hasAutocompleteInstance()){
										fieldinstance = (Field)a;
									}
								}
							}
							else{
								fieldinstance = (Field)r.getFieldFullList(fieldName);
							}
							
							if(fieldinstance != null){
	
								JSONObject data = new JSONObject();
								data.put("sourceField", key);
								data.put("itemDisplayName", itemDisplayName);
								data.put("refname", refname);
								data.put("uri", uri);
								//JSONObject data=miniForURI(storage,creds,cache,refname,null,restrictions);
								/*
								if(!data.has("refid")){//incase of permissions errors try our best
									data.put("refid",refname);
									if(data.has("displayName")){
										String itemDisplayName=((Element)node).selectSingleNode("itemDisplayName").getText();
										String temp = data.getString("displayName");
										data.remove("displayName");
										data.put(temp, itemDisplayName);
									}
								}
								*/
								data.put("sourceFieldselector", fieldinstance.getSelector());
								data.put("sourceFieldName", fieldName);
								data.put("sourceFieldType", r.getID());
								if(listitems.has(key)){
									JSONArray temp = listitems.getJSONArray(key).put(data);
									listitems.put(key,temp);
								}
								else{
									JSONArray temp = new JSONArray();
									temp.put(data);
									listitems.put(key,temp);
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
			out.put("listItems", listitems);
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
	public JSONObject refObjViewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String path, Record vr) throws ExistException, UnderlyingStorageException, JSONException, UnimplementedException {

		JSONObject out=new JSONObject();
			try{

			Map<String,String> reset_good=new HashMap<String,String>();// map of servicenames of fields to descriptors
			Map<String,String> reset_map=new HashMap<String,String>(); // map of csid to service name of field
			Map<String,String> old_good=view_good;// map of servicenames of fields to descriptors
			Map<String,String> old_map=view_map; // map of csid to service name of field
			Set<String> old_deurn=xxx_view_deurn;
			Map<String,List<String>>old_merge =view_merge;
			Set<String> reset_deurn=new HashSet<String>();
			Map<String,List<String>> reset_merge = new HashMap<String, List<String>>();
			if(vr.hasRefObjUsed()){
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
				JSONObject data = getRepeatableListView(storage,creds,cache,path,nodeName,"/authority-ref-doc-list/authority-ref-doc-item","uri", true, vr);//XXX this might be the wrong record to pass to checkf or hard/soft delet listing

				reset_good = view_good;
				reset_map = view_map;
				reset_deurn = xxx_view_deurn;
				reset_merge = view_merge;
				
				JSONArray recs = data.getJSONArray("listItems");
				//String[] filepaths = (String[]) data.get("listItems");
				for (int i = 0; i < recs.length(); ++i) {

					String uri = recs.getJSONObject(i).getString("csid");
					String filePath = recs.getJSONObject(i).getString("csid");
					if(filePath!=null && filePath.startsWith("/"))
						filePath=filePath.substring(1);
					
					String[] parts=filePath.split("/");
					String recordurl = parts[0];
					Record thisr = vr.getSpec().getRecordByServicesUrl(recordurl);
					resetGlean(thisr,reset_good,reset_map,reset_deurn,reset_merge, true);// what glean info required for this one..
					String csid = parts[parts.length-1];
					JSONObject dataitem = null;
					if(thisr.isType("authority")){
						dataitem =  miniViewRetrieveJSON(cache,creds,csid, "terms", thisr.getServicesURL()+"/"+uri, thisr);
					}
					else{
						dataitem =  miniViewRetrieveJSON(cache,creds,csid, "terms", uri, thisr);
					}
					//JSONObject 
					dataitem.getJSONObject("summarylist").put("uri",filePath);
					
					
					String key = recs.getJSONObject(i).getString("sourceField");
					dataitem.getJSONObject("summarylist").put("sourceField",key);
					String fieldName = "unknown";
					String fieldSelector = "unknown";
					if(key.contains(":")){
						fieldName = key.split(":")[1];
						//XXX fixCSPACE-2909 would be nice if they gave us the actual field rather than the parent
						//XXX CSPACE-2586
						while(thisr.getFieldFullList(fieldName) instanceof Repeat || thisr.getFieldFullList(fieldName) instanceof Group ){
							fieldName = ((Repeat)thisr.getFieldFullList(fieldName)).getChildren("GET")[0].getID();
						}
						Field fieldinstance = (Field)thisr.getFieldFullList(fieldName);
						fieldSelector = fieldinstance.getSelector();
					}

					dataitem.put("csid", csid);
					dataitem.put("sourceFieldselector", fieldSelector);
					dataitem.put("sourceFieldName", fieldName);
					dataitem.put("sourceFieldType", dataitem.getJSONObject("summarylist").getString("docType"));
					dataitem.put("sourceFieldType", dataitem.getJSONObject("summarylist").getString("docType"));
					
					out.put(csid+":"+key,dataitem);
				}
			}

			view_good = old_good;
			view_map = old_map;
			xxx_view_deurn = old_deurn;
			view_merge = old_merge;
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
		return refObjViewRetrieveJSON(storage,creds, cache,path, this.r);
	}
	
	/**
	 * update the item
	 * @param root
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param jsonObject
	 * @param thisr
	 * @param serviceurl
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
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
				//	log.info(doc.asXML());
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
			
			//XXX Completely untested subrecord update
			for(FieldSet fs : thisr.getAllSubRecords("PUT")){
				Record sr = fs.usesRecordId();
				if(sr.isRealRecord()){//only deal with ones which are separate Records in the services

					//get list of existing subrecords
					JSONObject existingcsid = new JSONObject();
					JSONObject updatecsid = new JSONObject();
					JSONArray createcsid = new JSONArray();
					String getPath = serviceurl+filePath + "/" + sr.getServicesURL();
					String node = "/"+sr.getServicesListPath().split("/")[0]+"/*";
					
					Integer subcount = 0;
					String firstfile = "";

					while(!getPath.equals("")){
						JSONObject data = getListView(creds,cache,getPath,node,"/"+sr.getServicesListPath(),"csid",false, sr);
						String[] filepaths = (String[]) data.get("listItems");
						subcount +=filepaths.length;
						if(firstfile.equals("") && subcount !=0){
							firstfile = filepaths[0];
						}
						//need to paginate
						for(String uri : filepaths) {
							String path = uri;
							if(path!=null && path.startsWith("/"))
								path=path.substring(1);
							existingcsid.put(path,"original");
						}
						if(data.has("pagination")){
							Integer ps = Integer.valueOf(data.getJSONObject("pagination").getString("pageSize"));
							Integer pn = Integer.valueOf(data.getJSONObject("pagination").getString("pageNum"));
							Integer ti = Integer.valueOf(data.getJSONObject("pagination").getString("totalItems"));
							if(ti > (ps * (pn +1))){
								JSONObject restrictions = new JSONObject();
								restrictions.put("pageSize", Integer.toString(ps));
								restrictions.put("pageNum", Integer.toString(pn + 1));

								getPath = getRestrictedPath(getPath, restrictions, sr.getServicesSearchKeyword(), "", false, "");
								//need more values
							}
							else{
								getPath = "";
							}
						}
					}

					//how does that compare to what we need
					if(sr.isType("authority")){
						//XXX need to use configuredVocabStorage
					}
					else{
						if(fs instanceof Field){
							JSONObject subdata = new JSONObject();
							//loop thr jsonObject and find the fields I need
							for(FieldSet subfs: sr.getAllFieldTopLevel("PUT")){
								String key = subfs.getID();
								if(jsonObject.has(key)){
									subdata.put(key, jsonObject.get(key));
								}
							}

							if(subcount ==0){
								//create
								createcsid.put(subdata);
							}
							else{
								//update - there should only be one
								String firstcsid = firstfile;
								updatecsid.put(firstcsid, subdata);
								existingcsid.remove(firstcsid);
							}
						}
						else if(fs instanceof Group){//JSONObject
							//do we have a csid
							//subrecorddata.put(value);
							if(jsonObject.has(fs.getID())){
								Object subdata = jsonObject.get(fs.getID());
								if(subdata instanceof JSONObject){
									if(((JSONObject) subdata).has("_subrecordcsid")){
										String thiscsid = ((JSONObject) subdata).getString("_subrecordcsid");
										//update
										if(existingcsid.has(thiscsid)){
											updatecsid.put(thiscsid, (JSONObject) subdata);
											existingcsid.remove(thiscsid);
										}
										else{
											//something has gone wrong... best just create it from scratch
											createcsid.put(subdata);
										}
									}
									else{
										//create
										createcsid.put(subdata);
									}
								}
							}
						}
						else{//JSONArray Repeat
							//need to find if we have csid's for each one
							if(jsonObject.has(fs.getID())){
								Object subdata = jsonObject.get(fs.getID());
								if(subdata instanceof JSONArray){
									JSONArray subarray = (JSONArray)subdata;

									for(int i=0;i<subarray.length();i++) {
										JSONObject subrecord = subarray.getJSONObject(i);
										if(((JSONObject) subrecord).has("_subrecordcsid")){
											String thiscsid = ((JSONObject) subrecord).getString("_subrecordcsid");
											//update
											if(existingcsid.has(thiscsid)){
												updatecsid.put(thiscsid, (JSONObject) subrecord);
												existingcsid.remove(thiscsid);
											}
											else{
												//something has gone wrong... best just create it from scratch
												createcsid.put(subrecord);
											}
										}
										else{
											//create
											createcsid.put(subrecord);
										}
									}
								}
							}
						}
						

						String savePath = serviceurl+filePath + "/" + sr.getServicesURL()+"/";
						
						//do delete JSONObject existingcsid = new JSONObject();
						Iterator<String> rit=existingcsid.keys();
						while(rit.hasNext()) {
							String key=rit.next();
							//should we delete or do we need to check that it is used? e.g. termlists
							if(sr.getID().equals("termlistitem")){
								//
								String subpath = savePath+key+"/refObjs";
								JSONObject test =  refObjViewRetrieveJSON(root,creds,cache,subpath, sr);
								if(test.length() > 0){
									throw new ExistException("Term List in use - can not delete: "+key);
								}
							}
							deleteJSON(root,creds,cache,key,savePath,sr);
						}
						
						//do update JSONObject updatecsid = new JSONObject();
						Iterator<String> keys = updatecsid.keys();
						while(keys.hasNext()) {
							String key=keys.next();
							JSONObject value = updatecsid.getJSONObject(key);
							updateJSON( root, creds, cache, key,  value, sr, savePath);
						}
						
						
						//do create JSONArray createcsid = new JSONArray();
						for(int i=0;i<createcsid.length();i++){
							JSONObject value = createcsid.getJSONObject(i);
							subautocreateJSON(root,creds,cache,sr,value,savePath);
						}
					}
					
				}
				
			}
			//if(status==404)
			//	throw new ExistException("Not found: "+serviceurl+filePath);
			if(status>299 || status<200)
				throw new UnderlyingStorageException("Bad response ",status,serviceurl+filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnimplementedException("UnsupportedEncodingException",e);
		}
	}
	
	/**
	 * guess service url based on record type and call the function with more parameters
	 * @param root
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param jsonObject
	 * @param thisr
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject,Record thisr)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		updateJSON( root, creds, cache, filePath,  jsonObject, thisr, thisr.getServicesURL()+"/");
	}

	/**
	 * guess the record type based on how this storage was initialized
	 * I allowed the specification of record types as sub records caused issues
	 * possibly should rework to keep record purity and think of a better way of calling the sub record info
	 */
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		updateJSON( root, creds, cache, filePath,  jsonObject, r);
	}
	
	/** 
	 * needs some tests.. just copied from ConfiguredVocabStorage
	 * @param root
	 * @param creds
	 * @param cache
	 * @param myr
	 * @param jsonObject
	 * @param savePrefix
	 * @return
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public String subautocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,Record myr,JSONObject jsonObject, String savePrefix)
	throws ExistException, UnimplementedException, UnderlyingStorageException{
		try {

			ReturnedURL url = null;
			Document doc = null;
			//used by userroles and permroles as they have complex urls
			//XXX I would hope this might be removed if userroles etc ever get improved to be more like the rest
			if(myr.hasPrimaryField()){
				//XXX test if works: need to delete first before create/update
			//	deleteJSON(root,creds,cache,filePath);

				for(String section : myr.getServicesRecordPaths()) {
					doc=XmlJsonConversion.convertToXml(myr,jsonObject,section,"POST");
					String path = myr.getServicesURL();
					path = path.replace("*", getSubCsid(jsonObject,myr.getPrimaryField()));

					deleteJSON(root,creds,cache,path);
					url = conn.getURL(RequestMethod.POST, path, doc, creds, cache);	
				}
			}
			else{
				url = autoCreateSub(creds, cache, jsonObject, doc, savePrefix, myr);
			}
			
			// create related sub records?
			for(FieldSet allfs : myr.getAllSubRecords("POST")){
				Record sr = allfs.usesRecordId();
				if(sr.isType("authority")){
				}
				else{
					String savePath = url.getURL() + "/" + sr.getServicesURL();
					if(jsonObject.has(sr.getID())){
						Object subdata = jsonObject.get(sr.getID());

						if(subdata instanceof JSONArray){
							JSONArray subarray = (JSONArray)subdata;

							for(int i=0;i<subarray.length();i++) {
								JSONObject subrecord = subarray.getJSONObject(i);
								subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
							}
							
						}
						else if(subdata instanceof JSONObject){
							JSONObject subrecord = (JSONObject)subdata;
							subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
						}
					}
				}
			}
			
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}
	
	/**
	 * Convert the JSON from the UI Layer into XML for the Service layer while using the XML structure from cspace-config.xml
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
				url = autoCreateSub(creds, cache, jsonObject, doc, r.getServicesURL(), r);
			}

			// create related sub records?
			//I am developing this.. it might not work...
			for(FieldSet fs : r.getAllSubRecords("POST")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					//need to use code from configuredVocabStorage
				}
				else{
					String savePath =  url.getURL() + "/" + sr.getServicesURL();
					if(fs instanceof Field){//get the fields form inline XXX untested - might not work...
						JSONObject subdata = new JSONObject();
						//loop thr jsonObject and find the fields I need
						for(FieldSet subfs: sr.getAllFieldTopLevel("POST")){
							String key = subfs.getID();
							if(jsonObject.has(key)){
								subdata.put(key, jsonObject.get(key));
							}
						}
						subautocreateJSON(root,creds,cache,sr,subdata,savePath);
					}
					else if(fs instanceof Group){//JSONObject
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONObject){
								JSONObject subrecord = (JSONObject)subdata;
								subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
							}
						}
					}
					else{//JSONArray
						if(jsonObject.has(fs.getID())){
							Object subdata = jsonObject.get(fs.getID());
							if(subdata instanceof JSONArray){
								JSONArray subarray = (JSONArray)subdata;

								for(int i=0;i<subarray.length();i++) {
									JSONObject subrecord = subarray.getJSONObject(i);
									subautocreateJSON(root,creds,cache,sr,subrecord,savePath);
								}
								
							}
						}
					}
				}
			}
			return url.getURLTail();
		} catch (ConnectionException e) {
			String msg = e.getMessage();
			if(e.getStatus() == 403){ //permissions error
				msg += " permissions error";
			}
			throw new UnderlyingStorageException(msg,e.getStatus(), e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}

	/**
	 * create sub records so can be connected to their parent record 
	 * e.g. blob/media contact/person
	 * @param creds
	 * @param cache
	 * @param jsonObject
	 * @param doc
	 * @param savePrefix
	 * @param r
	 * @return
	 * @throws JSONException
	 * @throws UnderlyingStorageException
	 * @throws ConnectionException
	 */
	protected ReturnedURL autoCreateSub(CSPRequestCredentials creds,
			CSPRequestCache cache, JSONObject jsonObject, Document doc, String savePrefix, Record r)
			throws JSONException, UnderlyingStorageException,
			ConnectionException {
		ReturnedURL url;
		Map<String,Document> parts=new HashMap<String,Document>();
		Document doc2 = doc;
		for(String section : r.getServicesRecordPaths()) {
			String path=r.getServicesRecordPath(section);
			String[] record_path=path.split(":",2);
			doc2=XmlJsonConversion.convertToXml(r,jsonObject,section,"POST");
			if(doc2!=null){
				doc = doc2;
				parts.put(record_path[0],doc2);
				//log.info(doc.asXML());
				//log.info(savePrefix);
			}
		}
		//some records are accepted as multipart in the service layers, others arent, that's why we split up here
		if(r.isMultipart())
			url = conn.getMultipartURL(RequestMethod.POST,savePrefix,parts,creds,cache);
		else
			url = conn.getURL(RequestMethod.POST, savePrefix, doc, creds, cache);

		if(url.getStatus()>299 || url.getStatus()<200)
			throw new UnderlyingStorageException("Bad response ",url.getStatus(),savePrefix);
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

	/**
	 * purposefully unimplemented functionality
	 */
	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot post to full path");
	}

	/**
	 * Split soft and hard delete into different functions
	 * you should not call this directly as need to check if soft or hard delete first
	 * use deleteJSON first
	 * @param root
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param serviceurl
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public void hardDeleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, String serviceurl) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			int status=conn.getNone(RequestMethod.DELETE,serviceurl+filePath,null,creds,cache);
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception",status,serviceurl+filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}		
	}
	
	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds,
			CSPRequestCache cache, String filePath, String serviceurl, String workflowTransition) 
					throws UnderlyingStorageException {
		try {
			String url = serviceurl+filePath+WORKFLOW_SUBRESOURCE+"/"+workflowTransition;
			int status = conn.getNone(RequestMethod.PUT, url, null, creds, cache);
			if(status>299 || status<200)
				throw new UnderlyingStorageException("Bad response ",status,serviceurl+filePath);
		}
		catch (ConnectionException e) {	
			throw new UnderlyingStorageException("Service layer exception (workflow transition)"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
		
	}

	public void transitionWorkflowJSON(ContextualisedStorage root,CSPRequestCredentials creds,
			CSPRequestCache cache,String filePath, String workflowTransition) 
					throws UnderlyingStorageException {
		transitionWorkflowJSON(root, creds, cache, filePath, r.getServicesURL()+"/", workflowTransition);
	}

	/**
	 * umbrella function for delete
	 * also works out service url from record type passed
	 * works out what kind of delete should be used
	 * @param root
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param thisr
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		String serviceurl = thisr.getServicesURL()+"/";
		deleteJSON(root,creds,cache,filePath,serviceurl,thisr );
	}
	
	/**
	 * umbrella function for delete
	 * works out what kind of delete should be used
	 * @param root
	 * @param creds
	 * @param cache
	 * @param filePath
	 * @param serviceurl
	 * @param thisr
	 * @throws ExistException
	 * @throws UnimplementedException
	 * @throws UnderlyingStorageException
	 */
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, String serviceurl, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		if(thisr.hasSoftDeleteMethod()){
			transitionWorkflowJSON(root, creds, cache, filePath, serviceurl, WORKFLOW_TRANSITION_DELETE);
		}
		else{
			hardDeleteJSON(root,creds,cache,filePath,serviceurl);
		}
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

	/**
	 * One stop shop to work out what the restricted path is that should be sent to the service layer
	 * @param basepath
	 * @param restrictions
	 * @param keywordparam
	 * @param tail
	 * @param isVocab
	 * @param displayName
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 */
	protected String getRestrictedPath(String basepath, JSONObject restrictions, String keywordparam, String tail, Boolean isVocab, String displayName) throws UnsupportedEncodingException, JSONException{
		
		String postfix = "?";

		if (tail.length() > 0) {
			postfix += tail.substring(1) + "&";
		}
		
		String prefix=null;
		Boolean queryadded = false;
		if(restrictions!=null){

			if(isVocab && restrictions.has(displayName)){
				prefix=restrictions.getString(displayName); 
			}
			if(restrictions.has("keywords")) {
				/* Keyword search */
				String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
				postfix += keywordparam+"="+data+"&";
			} 
			if(restrictions.has("advancedsearch")) {
				/* Advanced search */
				String data=URLEncoder.encode(restrictions.getString("advancedsearch"),"UTF-8");
				postfix += "as="+data+"&";
			} 
			if(restrictions.has("sortKey")){//"summarylist.updatedAt"//movements_common:locationDate
				postfix += "sortBy="+restrictions.getString("sortKey");
				if(restrictions.has("sortDir")){//1" - ascending, "-1" - descending
					if(restrictions.getString("sortDir").equals("-1")){
						postfix += "+DESC";
					}
					else{
						postfix += "+ASC";
					}
				}
				postfix += "&";
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

		if(isVocab){
			if(prefix!=null && !queryadded){
				postfix+="pt="+URLEncoder.encode(prefix,"UTF8")+"&";
			}
		}

		postfix = postfix.substring(0, postfix.length()-1);
		if(postfix.length() == 0){postfix +="/";}
		
		//log.info(postfix);
		String path = basepath+postfix;
		return path;
	}

	/**
	 * Gets a list of csids of a certain type of record together with the pagination info
	 */
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String path = getRestrictedPath(r.getServicesURL(), restrictions, r.getServicesSearchKeyword(), "", false, "");
			
			String node = "/"+r.getServicesListPath().split("/")[0]+"/*";
			JSONObject data = getListView(creds,cache,path,node,"/"+r.getServicesListPath(),"csid",false,r);
			
			return data;
			
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}


	/**
	 * umbrella function to get repeatable lists 
	 * sorts out whether you should be viewing a list that includeds soft deleted items or not
	 * @param root
	 * @param creds
	 * @param cache
	 * @param path
	 * @param nodeName
	 * @param matchlistitem
	 * @param csidfield
	 * @param fullcsid
	 * @param r
	 * @return
	 * @throws ConnectionException
	 * @throws JSONException
	 */
	protected JSONObject getRepeatableListView(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid, Record r) throws ConnectionException, JSONException {
		if(r.hasHierarchyUsed("screen")){
			path = hierarchicalpath(path);
		}
		if(r.hasSoftDeleteMethod()){
			return getRepeatableSoftListView(root,creds,cache,path,nodeName, matchlistitem, csidfield, fullcsid);
		}
		else{
			return getRepeatableHardListView(root,creds,cache,path,nodeName, matchlistitem, csidfield, fullcsid);
		}
	}

	/**
	 * umbrella function to get lists 
	 * sorts out whether you should be viewing a list that includeds soft deleted items or not
	 * @param creds
	 * @param cache
	 * @param path
	 * @param nodeName
	 * @param matchlistitem
	 * @param csidfield
	 * @param fullcsid
	 * @param r
	 * @return
	 * @throws ConnectionException
	 * @throws JSONException
	 */
	protected JSONObject getListView(CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid, Record r) throws ConnectionException, JSONException {
		if(r.hasHierarchyUsed("screen")){
			path = hierarchicalpath(path);
		}
		if(r.hasSoftDeleteMethod()){
			return getSoftListView(creds,cache,path,nodeName, matchlistitem, csidfield, fullcsid);
		}
		else{
			return getHardListView(creds,cache,path,nodeName, matchlistitem, csidfield, fullcsid);
		}
	}
	
	/**
	 * logic to work out what the service url is needed for getting items not in soft delete
	 * @param path
	 * @return
	 */
	protected String softpath(String path){
		String softdeletepath = path;
		
		//does path include a ? if so add &wf_delete else add ?wf_delete
		if(path.contains("?")){
			softdeletepath += "&";
		}
		else{
			softdeletepath += "?";
		}
		softdeletepath += "wf_deleted=false";
		return softdeletepath;
	}
	/**
	 * logic to work out what the service url is needed to show hierarchical information
	 * @param path
	 * @return
	 */
	protected String hierarchicalpath(String path){
		String hierarchicalpath = path;
		
		//does path include a ? if so add &wf_delete else add ?wf_delete
		if(path.contains("?")){
			hierarchicalpath += "&";
		}
		else{
			hierarchicalpath += "?";
		}
		hierarchicalpath += "showRelations=true";
		return hierarchicalpath;
	}

	protected JSONObject getRepeatableSoftListView(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid) throws ConnectionException, JSONException {
		String softdeletepath = softpath(path);
		
		return getRepeatableHardListView(root,creds,cache,softdeletepath,nodeName, matchlistitem, csidfield, fullcsid);
	}
	protected JSONObject getSoftListView(CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid) throws ConnectionException, JSONException {
		String softdeletepath = softpath(path);
		
		return getHardListView(creds,cache,softdeletepath,nodeName, matchlistitem, csidfield, fullcsid);
	}
	
	/**
	 * return list view of items including hard deleted
	 * @param root
	 * @param creds
	 * @param cache
	 * @param path
	 * @param nodeName
	 * @param matchlistitem
	 * @param csidfield
	 * @param fullcsid
	 * @return
	 * @throws ConnectionException
	 * @throws JSONException
	 */
	protected JSONObject getRepeatableHardListView(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid) throws ConnectionException, JSONException {
		JSONObject out = new JSONObject();
		JSONObject pagination = new JSONObject();
		Document list=null;
		List<JSONObject> listitems=new ArrayList<JSONObject>();
		ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
		if(all.getStatus()!=200){
			//throw new StatusException(all.getStatus(),path,"Bad request during identifier cache map update: status not 200");
			throw new ConnectionException("Bad request during identifier cache map update: status not 200 is "+Integer.toString(all.getStatus()),all.getStatus(),path);
		}
		list=all.getDocument();
		
		List<Node> nodes=list.selectNodes(nodeName);
		if(matchlistitem.equals("roles_list/*") || matchlistitem.equals("permissions_list/*")){
			//XXX hack to deal with roles being inconsistent
			//XXX CSPACE-1887 workaround
			for(Node node : nodes) {
				if(node.matches(matchlistitem)){
					String csid = node.valueOf( "@csid" );
					JSONObject test = new JSONObject();
					test.put("csid", csid);
					listitems.add(test);
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
					JSONObject test = new JSONObject();
					for(Node field : fields) {
						if(csidfield.equals(field.getName())) {
							if(!fullcsid){
								int idx=csid.lastIndexOf("/");
								if(idx!=-1)
									csid=csid.substring(idx+1);
							}
							test.put("csid", csid);
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
								String gleanname = r.getServicesURL()+"/"+csid;
								if(csidfield.equals("uri")){
									gleanname = csid;
								}
								setGleanedValue(cache,gleanname,json_name,value);
								test.put(json_name, value);
							}
						}
					}
					listitems.add(test);
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
		out.put("listItems", listitems);
		return out;
	}
	
	/**
	 * return list view of items
	 * TODO make getHardListView and getRepeatableHardListView to share more code as they aren't different enough to warrant the level of code repeat
	 * @param creds
	 * @param cache
	 * @param path
	 * @param nodeName
	 * @param matchlistitem
	 * @param csidfield
	 * @param fullcsid
	 * @return
	 * @throws ConnectionException
	 * @throws JSONException
	 */
	protected JSONObject getHardListView(CSPRequestCredentials creds,CSPRequestCache cache,String path, String nodeName, String matchlistitem, String csidfield, Boolean fullcsid) throws ConnectionException, JSONException {
		JSONObject out = new JSONObject();
		JSONObject pagination = new JSONObject();
		Document list=null;
		List<String> listitems=new ArrayList<String>();
		ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
		if(all.getStatus()!=200){
			//throw new StatusException(all.getStatus(),path,"Bad request during identifier cache map update: status not 200");
			throw new ConnectionException("Bad request during identifier cache map update: status not 200 is "+Integer.toString(all.getStatus()),all.getStatus(),path);
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
