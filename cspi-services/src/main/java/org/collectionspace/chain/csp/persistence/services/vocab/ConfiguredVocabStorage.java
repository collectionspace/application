/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
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

public class ConfiguredVocabStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(ConfiguredVocabStorage.class);
	private ServicesConnection conn;
	private VocabInstanceCache vocab_cache;
	private URNProcessor urn_processor;
	private Record r;

	public ConfiguredVocabStorage(Record r,ServicesConnection conn) throws  DocumentException, IOException {
		super(r,conn);
		this.conn=conn;
		urn_processor=new URNProcessor(r.getURNSyntax());
		Map<String,String> vocabs=new ConcurrentHashMap<String,String>();
		for(Instance n : r.getAllInstances()) {
			vocabs.put(n.getTitleRef(),n.getTitle());
		}
		vocab_cache=new VocabInstanceCache(r,conn,vocabs);
		this.r=r;
	}

	private Document createEntry(String section,String namespace,String root_tag,JSONObject data,String vocab,String refname, Record r) throws UnderlyingStorageException, ConnectionException, ExistException, JSONException {
		Document out=XmlJsonConversion.convertToXml(r,data,section,"POST");
		if(out!=null){
			Element root=out.getRootElement();
			Element vocabtag=root.addElement(r.getInTag());
			if(vocab!=null){
				vocabtag.addText(vocab);
			}
			Element refnametag=root.addElement("refName");
			if(refname!=null)
				refnametag.addText(refname);
			if(r.isType("compute-displayname")) {
				Element dnc=root.addElement("displayNameComputed");
				dnc.addText("false");
			}
			log.debug("create Configured Vocab Entry"+out.asXML());
		}
		return out;
	}

	private String getDisplayNameKey() throws UnderlyingStorageException {
		Field dnf=(Field)r.getDisplayNameField();
		if(dnf==null)
			throw new UnderlyingStorageException("no display-name='yes' field");
		return dnf.getID();
	}

	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String name=jsonObject.getString(getDisplayNameKey());
			String vocab=vocab_cache.getVocabularyId(creds,cache,filePath);
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null,r);
				if(temp!=null){
					body.put(record_path[0],temp);
				}
			}	
			// First send without refid (don't know csid)	
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/"+r.getServicesURL()+"/"+vocab+"/items",body,creds,cache);		
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary",out.getStatus(),"/"+r.getServicesURL()+"/"+vocab+"/items");
			// This time with refid
			String csid=out.getURLTail();
			String refname=urn_processor.constructURN("id",vocab,"id",out.getURLTail(),name);
			body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,refname,r);
				if(temp!=null){
					body.put(record_path[0],temp);
				}
			}
			ReturnedMultipartDocument out2=conn.getMultipartXMLDocument(RequestMethod.PUT,"/"+r.getServicesURL()+"/"+vocab+"/items/"+csid,body,creds,cache);
			if(out2.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
			//id
			cache.setCached(getClass(),new String[]{"namefor",vocab,out.getURLTail()},name);
			cache.setCached(getClass(),new String[]{"reffor",vocab,out.getURLTail()},refname);
			cache.setCached(getClass(),new String[]{"csidfor",vocab,out.getURLTail()},out.getURLTail());
			
			
			// create related sub records?
			for(FieldSet fs : r.getAllSubRecords("POST")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String savePath = out.getURL() + "/" + sr.getServicesURL();
					if(fs instanceof Field){//get the fields form inline XXX untested - might not work...
						JSONObject subdata = new JSONObject();
						//loop thr jsonObject and find the fields I need
						for(FieldSet subfs: sr.getAllFields("POST")){
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
			
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}
	
	public String subautocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,Record myr,JSONObject jsonObject, String savePrefix)
	throws ExistException, UnimplementedException, UnderlyingStorageException{
		try {
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : myr.getServicesRecordPaths()) {
				String path=myr.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);

				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,null,null,myr);
				if(temp!=null){
					body.put(record_path[0],temp);
				}
				
			}	
				
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,savePrefix,body,creds,cache);		
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary",out.getStatus(),savePrefix);
			
			
			// create related sub records?
			for(FieldSet allfs : myr.getAllSubRecords("POST")){
				Record sr = allfs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String savePath = out.getURL() + "/" + sr.getServicesURL();
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
			
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}


	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			String vocab=vocab_cache.getVocabularyId(creds,cache,filePath.split("/")[0]);
			String url = generateURL(vocab,filePath.split("/")[1],"");
			int status=conn.getNone(RequestMethod.DELETE,url,null,creds,cache);
			if(status>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary",status,url);
			cache.removeCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]});
			cache.removeCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]});
			cache.removeCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]});
			cache.removeCached(getClass(),new String[]{"csidfor",vocab,filePath.split("/")[1]});
			//delete name and id versions from teh cache?
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}	
	}

	/**
	 * Returns JSON containing pagenumber, pagesize, itemsinpage, totalitems and the list of items itself 
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out = new JSONObject();
			List<String> list=new ArrayList<String>();
			String vocab=vocab_cache.getVocabularyId(creds,cache,rootPath);
			String url="/"+r.getServicesURL()+"/"+vocab+"/items";
			String postfix = "?";
			String prefix=null;
			Boolean queryadded = false;
			if(restrictions!=null){
				if(restrictions.has(getDisplayNameKey())){
					prefix=restrictions.getString(getDisplayNameKey()); 
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
			
			if(prefix!=null && !queryadded){
				postfix+="pt="+URLEncoder.encode(prefix,"UTF8")+"&";
			}
			postfix = postfix.substring(0, postfix.length()-1);
			
			url+=postfix;
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,url,null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve vocabularies",500,url);
			String[] tag_parts=r.getServicesListPath().split(",",2);
			
			JSONObject pagination = new JSONObject();
			List<Node> nodes = doc.selectNodes("/"+tag_parts[1].split("/")[0]+"/*");
			for(Node node : nodes){
				if(node.matches("/"+tag_parts[1])){
					String name=node.selectSingleNode("displayName").getText();
					String csid=node.selectSingleNode("csid").getText();
					String refName=null;
					if(node.selectSingleNode("refName")!=null){
						refName=node.selectSingleNode("refName").getText();
					}
					String shortIdentifier=null;
					if(node.selectSingleNode("shortIdentifier")!=null){
						shortIdentifier=node.selectSingleNode("shortIdentifier").getText();
					}
					if(prefix==null || name.toLowerCase().contains(prefix.toLowerCase())){
						list.add(csid);
					}
					cache.setCached(getClass(),new String[]{"namefor",vocab,csid},name);
					//why don't we use the one we are given?
					if(refName!=null){
						refName=urn_processor.constructURN("id",vocab,"id",csid,name);
					}
					cache.setCached(getClass(),new String[]{"reffor",vocab,csid},refName);
					cache.setCached(getClass(),new String[]{"shortId",vocab,csid},shortIdentifier);
				}else{
					pagination.put(node.getName(), node.getText());
				}
			}
			
			out.put("pagination", pagination);
			out.put("listItems", list.toArray(new String[0]));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?"+e.getLocalizedMessage());
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Error parsing JSON"+e.getLocalizedMessage());
		}
	}

	
	/**
	 * Returns a list of items
	 */
	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			List<String> out=new ArrayList<String>();
			String vocab=vocab_cache.getVocabularyId(creds,cache,rootPath);
			String url="/"+r.getServicesURL()+"/"+vocab+"/items";

			String postfix = "?";
			String prefix=null;
			if(restrictions!=null){
				if(restrictions.has(getDisplayNameKey())){
					prefix=restrictions.getString(getDisplayNameKey());
				}
				if(restrictions.has("pageSize")){
					postfix += "pgSz="+restrictions.getString("pageSize")+"&";
				}
				if(restrictions.has("pageNum")){
					postfix += "pgNum="+restrictions.getString("pageNum")+"&";
				}
			}
			if(prefix!=null){
				postfix+="pt="+URLEncoder.encode(prefix,"UTF8")+"&";
			}
			postfix = postfix.substring(0, postfix.length()-1);
			
			url+=postfix;
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,url,null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve vocabularies",500,url);
			String[] tag_parts=r.getServicesListPath().split(",",2);
			List<Node> objects=doc.getDocument().selectNodes(tag_parts[1]);
			
			for(Node object : objects) {
				String name=object.selectSingleNode("displayName").getText();
				String csid=object.selectSingleNode("csid").getText();
				if(prefix==null || name.toLowerCase().contains(prefix.toLowerCase()))
					out.add(csid);
				cache.setCached(getClass(),new String[]{"namefor",vocab,csid},name);
				//why don't we just use refName that is sent in the payload?
				String refname=urn_processor.constructURN("id",vocab,"id",csid,name);
				cache.setCached(getClass(),new String[]{"reffor",vocab,csid},refname);
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?"+e.getLocalizedMessage());
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Error parsing JSON"+e.getLocalizedMessage());
		}
	}

	private String generateURL(String vocab,String path,String extrapath,Record myr) throws ExistException, ConnectionException, UnderlyingStorageException {
		String url = myr.getServicesURL()+"/"+vocab+"/items/"+path+extrapath;
		return url;
	}
	private String generateURL(String vocab,String path,String extrapath) throws ExistException, ConnectionException, UnderlyingStorageException {
		String url = r.getServicesURL()+"/"+vocab+"/items/"+path+extrapath;
		return url;
	}

	private JSONObject urnGet(String vocab,String entry,String refname) throws JSONException, ExistException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		//use cache?
		out.put("recordtype",r.getWebURL());
		out.put("refid",refname);
		out.put("csid",entry);
		out.put("displayName",urn_processor.deconstructURN(refname,false)[5]);
		return out;
	}
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Integer num = 0;
			String[] parts=filePath.split("/");
			//deal with different url structures
			String vocab,csid;
			if("_direct".equals(parts[0])) {
				if("urn".equals(parts[1])) {
					//this isn't simple pattern matching
					return urnGet(parts[2],parts[3],parts[4]);
				}
				vocab=parts[2];
				csid=parts[3];
				num=4;
			} else {
				vocab=vocab_cache.getVocabularyId(creds,cache,parts[0]);
				csid=parts[1];	
				num = 2;
			}		
			
			
			if(parts.length>num) {
				String extra = "";
				Integer extradata = num + 1;
				if(parts.length>extradata){
					extra = parts[extradata];
				}
				return viewRetrieveJSON(root,creds,cache,vocab,csid,parts[num],extra, restrictions);
			} else
				return simpleRetrieveJSON(root,creds,cache,vocab,csid);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}  catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON"+x.getLocalizedMessage(),x);
		}  catch (UnsupportedEncodingException x) {
			throw new UnderlyingStorageException("Error UnsupportedEncodingException JSON",x);
		}
		
	}
	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab, String csid,String view, String extra, JSONObject restrictions) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		try{
			if(view.equals("view")){
				return miniViewRetrieveJSON(storage, cache,creds,vocab, csid);
			}
			else if("authorityrefs".equals(view)){
				String path = generateURL(vocab,csid,"/authorityrefs");
				return refViewRetrieveJSON(storage,creds,cache,path,restrictions);
			}
			else if("refObjs".equals(view)){
				String path = generateURL(vocab,csid,"/refObjs");
				return refObjViewRetrieveJSON(storage,creds,cache,path);			
			}
			else
				return new JSONObject();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	
	
	public JSONObject miniViewRetrieveJSON(ContextualisedStorage storage,CSPRequestCache cache,CSPRequestCredentials creds,String vocab,String csid) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		try {
			JSONObject out=new JSONObject();
			//actually use cache			
			String name=(String)cache.getCached(getClass(),new String[]{"namefor",vocab,csid});
			String refid=(String)cache.getCached(getClass(),new String[]{"reffor",vocab,csid});
			String shortId=(String)cache.getCached(getClass(),new String[]{"shortId",vocab,csid});
			String testcsid=(String)cache.getCached(getClass(),new String[]{"csidfor",vocab,csid}); 
			//incase using nameurn
			if(testcsid!=null && testcsid.equals("") && !testcsid.equals(csid))
			{
				csid = testcsid;
			}
			if(name != null && refid != null && name.length() >0 && refid.length()>0 && shortId !=null){
				out.put(getDisplayNameKey(), name);
				out.put("refid", refid);
				out.put("csid",csid);
				out.put("authorityid", vocab);
				out.put("shortIdentifier", shortId);
				out.put("recordtype",r.getWebURL());
			}
			else{
				return simpleRetrieveJSON(storage, creds,cache,vocab,csid);
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	
	
	public JSONObject simpleRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab, String csid) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException{
		JSONObject out=new JSONObject();
		out=get(storage, creds,cache,vocab,csid);
		cache.setCached(getClass(),new String[]{"csidfor",vocab,csid},out.get("csid"));//cos csid might be a refname at this point..
		cache.setCached(getClass(),new String[]{"namefor",vocab,csid},out.get(getDisplayNameKey()));
		cache.setCached(getClass(),new String[]{"reffor",vocab,csid},out.get("refid"));
		cache.setCached(getClass(),new String[]{"shortId",vocab,csid},out.get("shortIdentifier"));
		return out;
	}
	
	private JSONArray get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid,String filePath, Record thisr) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		JSONArray itemarray = new JSONArray();
//get list view

		String node = "/"+thisr.getServicesListPath().split("/")[0]+"/*";
		JSONObject data = getListView(storage,creds,cache,filePath,node,"/"+thisr.getServicesListPath(),"csid",false);
		

		String[] filepaths = (String[]) data.get("listItems");
		for(String uri : filepaths) {
			String path = uri;
			if(path!=null && path.startsWith("/"))
				path=path.substring(1);
			
			String[] parts=path.split("/");
			String recordurl = parts[0];
			String mycsid = parts[parts.length-1];
			
			try {
				JSONObject itemdata= simpleRetrieveJSON( creds, cache, filePath+"/"+mycsid,"",  thisr);
				itemdata.put("_subrecordcsid", mycsid);//add in csid so I can do update with a modicum of confidence
				itemarray.put(itemdata);
			} catch (UnimplementedException e) {
				throw new UnderlyingStorageException(e.getMessage());
			}
		}
		return itemarray;
		
	}
	
	private JSONObject get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		int status=0;
		JSONObject out = new JSONObject();
			// XXX pagination support
			String url = generateURL(vocab,csid,"");
			ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,url,null,creds,cache);
			if(doc.getStatus()==404)
				throw new ExistException("Does not exist");
			if(doc.getStatus()>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus());
			String name = null;
			String refid = null;
			String shortIdentifier = "";
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				Document result=doc.getDocument(record_path[0]);
				if("common".equals(section)) { // XXX hardwired :(
					name=result.selectSingleNode(tag_path[1]+"/displayName").getText();
					if(result.selectSingleNode(tag_path[1]+"/shortIdentifier")!=null){
						shortIdentifier = result.selectSingleNode(tag_path[1]+"/shortIdentifier").getText();
					}
					refid=result.selectSingleNode(tag_path[1]+"/refName").getText();
					XmlJsonConversion.convertToJson(out,r,result,"GET",section);	
				}			
			}
			

			// get related sub records?
			for(FieldSet fs : r.getAllSubRecords("GET")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String getPath = url + "/" + sr.getServicesURL();
					JSONArray subout = get(storage, creds,cache,vocab,csid,getPath,sr);
					if(fs instanceof Field){
						JSONObject fielddata = subout.getJSONObject(0);

						Iterator<String> rit=fielddata.keys();
						while(rit.hasNext()) {
							String key=rit.next();
							out.put(key, fielddata.get(key));
						}
					}
					else if(fs instanceof Group){
						if(subout.length()>0){
							out.put(fs.getID(), subout.getJSONObject(0));
						}
					}
					else{
						out.put(fs.getID(), subout);
					}
				}
			}
			
			//deurn url incase we were sent name not id as csid
			String[] urned = urn_processor.deconstructURN(refid,false);
			//csid = urn_processor.deconstructURN(refid,false)[4];
			
			out.put(getDisplayNameKey(),name);
			out.put("csid",urned[4]);
			out.put("refid",refid);
			out.put("shortIdentifier", shortIdentifier);
			out.put("authorityid", vocab);
			out.put("recordtype",r.getWebURL());
			return out;
	}
	
	

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String name=jsonObject.getString(getDisplayNameKey());
			String vocab=vocab_cache.getVocabularyId(creds,cache,filePath.split("/")[0]);
			String csid = filePath.split("/")[1];
			String refname=urn_processor.constructURN("id",vocab,"id",csid,name);
			if(csid.startsWith(r.getURNVocab())){
				String shortid = csid.substring(r.getURNVocab().length() + 1, csid.length()-1);
				refname=urn_processor.constructURN("id",vocab,r.getURNVocab(),shortid,name);
			}
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);

				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,refname,r);
				if(temp!=null){
					body.put(record_path[0],temp);
				}
				
			}
			String url = generateURL(vocab,filePath.split("/")[1],"");
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,url,body,creds,cache);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary",out.getStatus(),url);
			cache.setCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]},name);
			cache.setCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]},refname);
			
			
			//subrecord update
			for(FieldSet fs : r.getAllSubRecords("PUT")){
				Record sr = fs.usesRecordId();
				
				//get list of existing subrecords
				JSONObject existingcsid = new JSONObject();
				JSONObject updatecsid = new JSONObject();
				JSONArray createcsid = new JSONArray();
				String getPath = url + "/" + sr.getServicesURL();
				String node = "/"+sr.getServicesListPath().split("/")[0]+"/*";
				JSONObject data = getListView(root,creds,cache,getPath,node,"/"+sr.getServicesListPath(),"csid",false);
				

				String[] filepaths = (String[]) data.get("listItems");
				for(String uri : filepaths) {
					String path = uri;
					if(path!=null && path.startsWith("/"))
						path=path.substring(1);
					existingcsid.put(path,"original");
				}
				
				//how does that compare to what we need
				if(sr.isType("authority")){
					if(fs instanceof Field){
						JSONObject subdata = new JSONObject();
						//loop thr jsonObject and find the fields I need
						for(FieldSet subfs: sr.getAllFields("PUT")){
							String key = subfs.getID();
							if(jsonObject.has(key)){
								subdata.put(key, jsonObject.get(key));
							}
						}

						if(filepaths.length ==0){
							//create
							createcsid.put(subdata);
						}
						else{
							//update - there should only be one
							String firstcsid = filepaths[0];
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
					}
					

					String savePath = url + "/" + sr.getServicesURL()+"/";
					
					//do delete JSONObject existingcsid = new JSONObject();
					Iterator<String> rit=existingcsid.keys();
					while(rit.hasNext()) {
						String key=rit.next();
						deleteJSON(root,creds,cache,key,savePath);
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
			
			
			//XXX dont currently update the shortID???
			//cache.setCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]},shortId);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}
}
