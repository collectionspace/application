/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldParent;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Relationship;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.util.json.JSONUtils;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.collectionspace.services.common.api.RefName;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
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
	private Record r;
	
	final static String XPATH_FIRST_EL = "[1]";
	final static String XPATH_GENERIC_FIRST_EL = "*[1]";
	final static String ITEMS_SUFFIX = "/items";
	final static String VOCAB_WILDCARD = "_ALL_";
	final static String ALL_VOCAB_ITEMS = "/"+VOCAB_WILDCARD+ITEMS_SUFFIX;
        
        final static String VOCABULARY_UPDATE_FAILED_MESSAGE = "Could not update vocabulary";

	public ConfiguredVocabStorage(Record r,ServicesConnection conn) throws  DocumentException, IOException {
		super(r,conn);
		initializeGlean(r);
		this.conn=conn;
		this.r=r;
	}
	
	private String getDisplayNameKey() throws UnderlyingStorageException {
		Field dnf=(Field)r.getDisplayNameField();
		if(dnf==null)
			throw new UnderlyingStorageException("no display-name='yes' field");
		return dnf.getID();
	}
	
	private String getDisplayNameXPath() throws UnderlyingStorageException {
		Field dnf=(Field)r.getDisplayNameField();
		if(dnf==null)
			throw new UnderlyingStorageException("no display-name='yes' field");
		return getXPathForField(dnf);
	}
	/**
	 * Returns an XPath-conformant string that specifies the full (X)path to this field.
	 * May recurse to handle nested fields. Will choose primary, in lists (i.e, 1st element)
	 * This should probably live in Field.java, not here (but needs to be in both Field.java 
	 * and Repeat.java - do I hear "Base Class"?!?!) 
	 * 
	 * @param fieldSet the containing fieldSet
	 * @return NXQL conformant specifier.
	 **/
	public static String getXPathForField(FieldSet fieldSet) {
		String specifier = fieldSet.getServicesTag();
		
		// Check for a composite (fooGroupList/fooGroup). For these, the name is the 
		// leaf, and the first part is held in the "services parent"
		if(fieldSet.hasServicesParent()) {
			// Prepend the services parent field, and make the child a wildcard
			String [] svcsParent = fieldSet.getServicesParent();
			if(svcsParent[0] != null && !svcsParent[0].isEmpty()) {
				specifier = svcsParent[0] + "/";
				// Note that we do not handle paths more the 2 in length - makes no sense
				if(svcsParent.length < 2) {
					specifier += XPATH_GENERIC_FIRST_EL;
				} else if(svcsParent[1] == null) { // Work around ridiculous hack/nonsense in Repeat init
					specifier += fieldSet.getServicesTag();
				} else {
					specifier += svcsParent[1];
				}
				specifier += XPATH_FIRST_EL;
			}
		}
		
		FieldParent parent = fieldSet.getParent();

		boolean isRootLevelField = false;			// Assume we are recursing until we see otherwise
		if(parent instanceof Record) {	// A simple reference to base field.
			isRootLevelField = true;
			log.debug("Specifier for root-level field: " + specifier + " is: "+specifier);
		} else {
			FieldSet parentFieldSet = (FieldSet)parent;
			// "repeator" marks things for some expansion - not handled here (?)
			if(parentFieldSet.getSearchType().equals("repeator")) {
				isRootLevelField = true;
			} else {
				// Otherwise, we're dealing with some amount of nesting.
				// First, recurse to get the fully qualified path to the parent.
				if(log.isDebugEnabled()) {
					String parentID = parentFieldSet.getID();
					log.debug("Recursing for parent: " + parentID );
				}
				specifier = getXPathForField(parentFieldSet);
							
				// Is parent a scalar list or a complex list?
				Repeat rp = (Repeat)parentFieldSet;
				FieldSet[] children = rp.getChildren("");
				int size = children.length;
				// HACK - we should really mark a repeating scalar as such, 
				// or a complex schema from which only 1 field is used, will break this.
				if(size > 1){
					// The parent is a complex schema, not just a scalar repeat
					// Append the field name to build an XPath-like specifier.
					specifier += "/"+fieldSet.getServicesTag();
				} else{
					// Leave specifier as is. We just search on the parent name,
					// as the backend is smart about scalar lists. 
				}
			}
			log.debug("Specifier for non-leaf field: " + fieldSet.getServicesTag() + " is: "+specifier);
		}
		if(isRootLevelField) {
			// TODO - map leaf names like "titleGroupList/titleGroup" to "titleGroupList/*"
		}
		return specifier;
	}

	private Document createEntry(String section, String namespace, String root_tag, JSONObject data, String vocab, String refname, Record r, Boolean isAuth) throws UnderlyingStorageException,
			ConnectionException, ExistException, JSONException {
		Document out = XmlJsonConversion.convertToXml(r, data, section, "POST", isAuth);
		if (section.equals("common")) {// XXX not great... but not sure how else
										// to differentiate
			if (out != null) {
				Element root = out.getRootElement();
				if (vocab != null && !vocab.isEmpty()) {
					Element vocabtag = root.addElement("inAuthority");
					if (vocab != null) {
						vocabtag.addText(vocab);
					}
				}
				
				if (refname != null) {
					// CSPACE-4460
					// Element refnametag=root.addElement("refName");
					// refnametag.addText(refname);
				}
				
				if (r.isType("compute-displayname")) {
					Element dnc = root.addElement("displayNameComputed");
					dnc.addText("false");
				}
				// log.info("create Configured Vocab Entry"+out.asXML());
			}
		}
		return out;
	}
	
	@Override
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject, JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Map<String,Document> body=new HashMap<String,Document>();
			String vocab = null;

			String pathurl ="/"+r.getServicesURL()+"/";
			if(filePath.equals("")){ //this creating an authority instance not an item 

				for(String section : r.getServicesInstancesPaths()) {
					String path=r.getServicesInstancesPath(section);
					String[] record_path=path.split(":",2);
					String[] tag_path=record_path[1].split(",",2);
					Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null,r,true);
					if(temp!=null){
						body.put(record_path[0],temp);
						//log.info(temp.asXML());
					}

				}
			}
			else{
				vocab = RefName.shortIdToPath(filePath);
				pathurl ="/"+r.getServicesURL()+"/"+vocab+ITEMS_SUFFIX;
				for(String section : r.getServicesRecordPathKeys()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					String[] tag_path=record_path[1].split(",",2);
					Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null,r, false);
					if(temp!=null){
						body.put(record_path[0],temp);
						//log.info(temp.asXML());
					}

				}
			}
	
			handleHierarchyPayloadSend(r, body, jsonObject, null);

			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,pathurl,body,creds,cache);		
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary",out.getStatus(),pathurl);


			String csid=out.getURLTail();
			//CACHE????? should we cache things?
			
			// create related sub records?
			for(FieldSet fs : r.getAllSubRecords("POST")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String savePath = out.getURL() + "/" + sr.getServicesURL();
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
							} else {
								log.warn("autocreateJSON: Contact subrecord is malformed (not a JSONObject)!");
								if(log.isDebugEnabled()) {
									log.debug("autocreateJSON: Contact subrecord: "+subdata.toString());
								}
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
			return csid;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON"+e.getLocalizedMessage(),e);
		}
	}
	

	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Integer num = 0;
			String[] parts=filePath.split("/");
			//deal with different url structures
			String vocab, csid;
			if("_direct".equals(parts[0])) { 
				vocab=parts[2];
				csid=parts[3];
				num=4;
			} else {
				vocab=parts[0];
				if(!VOCAB_WILDCARD.equals(vocab)) {
					vocab = RefName.shortIdToPath(vocab);
				}
				csid = (parts.length > 1) ? parts[1] : null;
				num = 2;
			}		
			
			if(parts.length>num) {
				String extra = "";
				Integer extradata = num + 1;
				if(parts.length>extradata){
					extra = parts[extradata];
				}
				String servicepath = generateURL(vocab,csid,"",this.r);
				
				return viewRetrieveJSON(root,creds,cache,null,parts[num], extra, restrictions, servicepath);
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
	
	private String generateURL(String vocab,String path,String extrapath,Record myr) throws ExistException, ConnectionException, UnderlyingStorageException {
		String url = myr.getServicesURL()+"/"+vocab+"/items/"+path+extrapath;
		return url;
	}
	
	public JSONObject simpleRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab, String csid) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException{
		JSONObject out=new JSONObject();
		
		if (csid == null) {
			out = getInstance(storage, creds, cache, vocab, conn.getIMSBase());
		}
		else {
			out=get(storage, creds,cache,vocab,csid,conn.getIMSBase());
			//cache.setCached(getClass(),new String[]{"csidfor",vocab,csid},out.get("csid"));//cos csid might be a refname at this point..
			//cache.setCached(getClass(),new String[]{"namefor",vocab,csid},out.get(getDisplayNameKey()));
			//cache.setCached(getClass(),new String[]{"reffor",vocab,csid},out.get("refid"));
			//cache.setCached(getClass(),new String[]{"shortId",vocab,csid},out.get("shortIdentifier"));
		}
		
		return out;
	}
	
	private JSONObject getInstance(ContextualisedStorage storage, CSPRequestCredentials creds, CSPRequestCache cache, String vocab, String ims_url) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		String url = this.r.getServicesURL()+ "/" + vocab;
		String csid = "";
		JSONObject out = new JSONObject();
		String softurl = url;
		
		if(r.hasSoftDeleteMethod()){
			softurl = softpath(url);
		}
		if(r.hasHierarchyUsed("screen")){
			softurl = hierarchicalpath(softurl);
		}
			
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,softurl,null,creds,cache);

		if (doc.getStatus() == 404) {
			throw new ExistException("Does not exist " + softurl);
		}
		
		if (doc.getStatus() == 403) {
			// permission error
			return out;
		}
		
		if (doc.getStatus () > 299) {
			throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus(),
					doc.getStatus(), softurl);
		}
		
		String name = null;
		String refid = null;
		String shortIdentifier = "";
		
		for(String section : r.getServicesRecordPathKeys()) {
			String path = r.getServicesInstancesPath(section);
			
			if (path == null) {
				path = r.getServicesRecordPath(section);
			}

			String[] record_path = path.split(":",2);
			String[] tag_path = record_path[1].split(",",2);
			Document result = doc.getDocument(record_path[0]);
	
			if("common".equals(section)) { // XXX hardwired :(
				name = result.selectSingleNode(tag_path[1] + "/displayName").getText();
				
				if (result.selectSingleNode(tag_path[1]+"/shortIdentifier") != null) {
					shortIdentifier = result.selectSingleNode(tag_path[1] + "/shortIdentifier").getText();
				}
				
				refid = result.selectSingleNode(tag_path[1] + "/refName").getText();
				
				csid=result.selectSingleNode(tag_path[1]+"/csid").getText();
			}
			else if ("collectionspace_core".equals(section)) {
				XmlJsonConversion.convertToJson(out, r, result, "GET", section, csid, ims_url);
			}
		}
		
		out.put("displayName", name);
		out.put("csid", csid);
		out.put("refid", refid);
		out.put("shortIdentifier", shortIdentifier);
		
		return out;
	}

	private JSONObject get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid,String ims_url) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		String url = generateURL(vocab,csid,"",this.r);
		return get(storage,creds,cache,url,ims_url);
	}
	private JSONObject get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String url,String ims_url) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		//int status=0;
		String csid = "";
		JSONObject out = new JSONObject();
			// XXX pagination support

			String softurl = url;
			if(r.hasSoftDeleteMethod()){
				softurl = softpath(url);
			}
			if(r.hasHierarchyUsed("screen")){
				softurl = hierarchicalpath(softurl);
			}
			
			
			ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,softurl,null,creds,cache);
			if(doc.getStatus()==404)
				throw new ExistException("Does not exist "+softurl);
			if(doc.getStatus()==403){
				//permission error - keep calm and carry on with what we can glean
				out.put("displayName",getDisplayNameKey());
				out.put("csid",csid);
				out.put("recordtype",r.getWebURL());
				return out;
			}
			if(doc.getStatus()>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus(),
						doc.getStatus(), softurl);
			String name = null;
			String refid = null;
			String termStatus = null;
			String parentcsid = null;
			String shortIdentifier = "";
			
			for(String section : r.getServicesRecordPathKeys()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				Document result=doc.getDocument(record_path[0].trim());

				if (result != null) {
					if("common".equals(section)) { // XXX hardwired :(
						String dnXPath = getDisplayNameXPath();
						name=result.selectSingleNode(tag_path[1]+"/"+dnXPath).getText();
						if(result.selectSingleNode(tag_path[1]+"/shortIdentifier")!=null){
							shortIdentifier = result.selectSingleNode(tag_path[1]+"/shortIdentifier").getText();
						}
						refid=result.selectSingleNode(tag_path[1]+"/refName").getText();
						// We need to replace this with the same model as for displayName
						if(result.selectSingleNode(tag_path[1]+"/termStatus") != null){
						termStatus=result.selectSingleNode(tag_path[1]+"/termStatus").getText();
						}
						else{
							termStatus = "";
						}
						csid=result.selectSingleNode(tag_path[1]+"/csid").getText();
						parentcsid = result.selectSingleNode(tag_path[1]+"/inAuthority").getText();
						XmlJsonConversion.convertToJson(out,r,result,"GET",section,csid,ims_url);	
					}
					else{
						XmlJsonConversion.convertToJson(out,r,result,"GET",section,csid,ims_url);
					}
				} else {
					log.warn(String.format("XML Payload for '%s' was missing part '%s'.", url, record_path[0]));
				}
			}
			
			// If this record has hierarchy, will pull out the relations section and map it to the hierarchy
			// fields (special case handling of XML-JSON
			handleHierarchyPayloadRetrieve(r, doc, out, csid);

			// get related sub records?
			for(FieldSet fs : r.getAllSubRecords("GET")){
				Record sr = fs.usesRecordId();
				//sr.getID()
				if(sr.isType("authority")){
					String getPath = url + "/" + sr.getServicesURL();
					JSONArray subout = get(storage, creds,cache,url,getPath,sr);
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
			
			//csid = urn_processor.deconstructURN(refid,false)[4];
			
			out.put(getDisplayNameKey(),name);
			out.put("csid",csid);
			out.put("refid",refid);
			RefName.AuthorityItem item = RefName.AuthorityItem.parse(refid); 
			out.put("namespace",item.getParentShortIdentifier());
			out.put("shortIdentifier", shortIdentifier);
			out.put("termStatus", termStatus);
			out.put("authorityid", parentcsid);
			out.put("recordtype",r.getWebURL());
			return out;
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
			String url;
			if(rootPath.isEmpty()) {
				url="/"+r.getServicesURL()+ALL_VOCAB_ITEMS;
			} else {
				String vocab = RefName.shortIdToPath(rootPath);
				url="/"+r.getServicesURL()+"/"+vocab+ITEMS_SUFFIX;
			}
			
			String path = getRestrictedPath(url, restrictions, r.getServicesSearchKeyword(), "", true, getDisplayNameKey() );

			boolean excludeSoftDeleted = true;
			
			if (restrictions.has("deleted")) {
				excludeSoftDeleted = !restrictions.getBoolean("deleted");
			}

			if(excludeSoftDeleted && r.hasSoftDeleteMethod()){
				path = softpath(path);
			}
			
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
			Document doc=data.getDocument();

			if (doc == null) {
				throw new UnderlyingStorageException("Could not retrieve vocabulary items",data.getStatus(),path);
			}
			
			String[] tag_parts=r.getServicesListPath().split(",",2);
			String listItemPath = tag_parts[1]; 
			
			String[] listItemPathElements = listItemPath.split("/");
			
			if (listItemPathElements.length != 2) {
				throw new RuntimeException("Illegal list item path " + listItemPath);
			}

			String listNodeName = listItemPathElements[0];
			String listItemNodeName = listItemPathElements[1];

			String listNodeChildrenSelector = "/"+listNodeName+"/*";

			JSONObject pagination = new JSONObject();
			String[] allfields = null;
			String fieldsReturnedName = r.getServicesFieldsPath();
			List<Node> nodes = doc.selectNodes(listNodeChildrenSelector);
			for(Node node : nodes){
				if(listItemNodeName.equals(node.getName())){
					// Risky hack - assumes displayName must be at root. Really should
					// understand that the list results are a different schema from record GET.
					String dnName = getDisplayNameKey();
					String csid=node.selectSingleNode("csid").getText();
					list.add(csid);
					String urlPlusCSID = url+"/"+csid;
					
					List<Node> nameNodes=node.selectNodes(dnName);
					String nameListValue = null;
					for(Node nameNode : nameNodes) {
						String name=nameNode.getText();
						if(nameListValue == null) {
							nameListValue = name;
						} else {
							nameListValue = JSONUtils.appendWithArraySeparator(nameListValue, name); 
						}
					}
					if(nameListValue==null) {
						throw new JSONException("No displayNames found!");
					} else {
						String json_name=view_map.get(dnName);
						setGleanedValue(cache,urlPlusCSID,json_name,nameListValue);
					}

					List<Node> fields=node.selectNodes("*[(name()!='"+dnName+"')]");
					for(Node field : fields) {
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
							setGleanedValue(cache,urlPlusCSID,json_name,value);
						}
					}
					if(allfields==null || allfields.length==0) {
						log.warn("Missing fieldsReturned value - may cause fan-out!");
					} else {
						// Mark all the fields not yet found as gleaned - 
						for(String s : allfields){
							String gleaned = getGleanedValue(cache,urlPlusCSID,s);
							if(gleaned==null){
								setGleanedValue(cache,urlPlusCSID,s,"");
							}
						}
					}
				} else if(fieldsReturnedName.equals(node.getName())){
					String myfields = node.getText();
					allfields = myfields.split("\\|");
				} else {
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
	
	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			String vocab = RefName.shortIdToPath(filePath.split("/")[0]);
			String url = generateURL(vocab,filePath.split("/")[1],"",this.r);

			if(r.hasSoftDeleteMethod()){
				// The url we compute already has the filepath built in, so just pass an empty
				// filepath, and it will work out.
				String emptyFilepath = "";
				super.transitionWorkflowJSON(root, creds, cache, emptyFilepath, url, WORKFLOW_TRANSITION_DELETE);
			}
			else{
				int status=conn.getNone(RequestMethod.DELETE,url,null,creds,cache);
				if(status>299)
					throw new UnderlyingStorageException("Could not retrieve vocabulary",status,url);
			}
			//cache.removeCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]});
			//cache.removeCached(getClass(),new String[]{"csidfor",vocab,filePath.split("/")[1]});
			//delete name and id versions from teh cache?
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}	
	}
	
	public void transitionWorkflowJSON(ContextualisedStorage root, CSPRequestCredentials creds,
		CSPRequestCache cache, String filePath, String serviceurl, String workflowTransition) 
				throws UnderlyingStorageException {
		String vocab = RefName.shortIdToPath(filePath.split("/")[0]);
		String url = null;
		
		try {
			url = generateURL(vocab,filePath.split("/")[1],"",this.r);
		}
		catch(ExistException e) {
			throw new UnderlyingStorageException("Exist exception"+e.getLocalizedMessage(),e.getStatus(),url,e);
		}
		catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
		
		super.transitionWorkflowJSON(root, creds, cache, "", url, workflowTransition);
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,
			String filePath, JSONObject jsonObject, JSONObject restrictions,
			Record thisr, String serviceurl)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		String vocab = RefName.shortIdToPath(filePath.split("/")[0]);
		String csid = filePath.split("/")[1];
		String savePath;
		try {
			savePath = generateURL(vocab,csid,"",thisr);
			updateJSON(root,creds,cache,jsonObject, restrictions, thisr, savePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception "+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	
	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, 
			JSONObject jsonObject, JSONObject restrictions,
			Record thisr, String savePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String csid = savePath.split("/")[3];
			
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPathKeys()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);

				Document temp = createEntry(section,tag_path[0],tag_path[1],jsonObject,null,null,thisr, false);
				if(temp!=null){
					body.put(record_path[0],temp);
					//log.info(temp.asXML());
				}
				
			}

			handleHierarchyPayloadSend(thisr, body, jsonObject, csid);
			
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,savePath,body,creds,cache);
			if(out.isErrorStatus()){
                            if(out.isTransactionFailedStatus()){
				throw new UnderlyingStorageException(VOCABULARY_UPDATE_FAILED_MESSAGE + ": " + out.TRANSACTION_FAILED_MESSAGE,out.getStatus(),savePath);
			    } else {
				throw new UnderlyingStorageException(VOCABULARY_UPDATE_FAILED_MESSAGE,out.getStatus(),savePath);
                            }
                        }

			//cache.setCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]},name);
			//cache.setCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]},refname);
			
			
			//subrecord update
			for(FieldSet fs : thisr.getAllSubRecords("PUT")){
				Record sr = fs.usesRecordId();
				
				//get list of existing subrecords
				JSONObject existingcsid = new JSONObject();
				JSONObject updatecsid = new JSONObject();
				JSONArray createcsid = new JSONArray();
				String getPath = savePath + "/" + sr.getServicesURL();
				Integer subcount = 0;
				String firstfile = "";

				while(!getPath.equals("")){
					JSONObject data = getListView(creds,cache,getPath,sr.getServicesListPath(),"csid",false, sr);
					String[] filepaths = (String[]) data.get("listItems");
					subcount +=filepaths.length;
					if(firstfile.equals("") && subcount !=0){
						firstfile = filepaths[0];
					}
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
							JSONObject pgRestrictions = new JSONObject();
							pgRestrictions.put("pageSize", Integer.toString(ps));
							pgRestrictions.put("pageNum", Integer.toString(pn + 1));

							getPath = getRestrictedPath(getPath, pgRestrictions, sr.getServicesSearchKeyword(), "", false, "");
							//need more values
						}
						else{
							getPath = "";
						}
					}
				}


				
				//how does that compare to what we need
				if(sr.isType("authority")){
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
									if(subrecord.has("_subrecordcsid")){
										String thiscsid = subrecord.getString("_subrecordcsid");
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
					

					String savePathSr = savePath + "/" + sr.getServicesURL()+"/";
					
					//do delete JSONObject existingcsid = new JSONObject();
					Iterator<String> rit=existingcsid.keys();
					while(rit.hasNext()) {
						String key=rit.next();
						deleteJSON(root,creds,cache,key,savePathSr,sr);
					}
					
					//do update JSONObject updatecsid = new JSONObject();
					Iterator<String> keys = updatecsid.keys();
					while(keys.hasNext()) {
						String key=keys.next();
						JSONObject value = updatecsid.getJSONObject(key);
						String thissave = savePathSr + key;

						updateJSON(root,creds,cache,value, new JSONObject(), sr, thissave);
						//updateJSON( root, creds, cache, key,  value, sr, savePathSr);
					}
					
					
					//do create JSONArray createcsid = new JSONArray();
					for(int i=0;i<createcsid.length();i++){
						JSONObject value = createcsid.getJSONObject(i);
						subautocreateJSON(root,creds,cache,sr,value,savePathSr);
					}
				}
			}
			
			
			//XXX dont currently update the shortID???
			//cache.setCached(getClass(),new String[]{"shortId",vocab,filePath.split("/")[1]},shortId);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception "+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON "+e.getLocalizedMessage(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnimplementedException("UnsupportedEncodingException"+e.getLocalizedMessage(),e);
		}
	}
	
	private JSONArray get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid,String filePath, Record thisr) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		String url = generateURL(vocab,csid,"",this.r);
		return get(storage,creds,cache,url,filePath,thisr);
	}
	private JSONArray get(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String url,String filePath, Record thisr) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		JSONArray itemarray = new JSONArray();
//get list view

		JSONObject data = getListView(creds,cache,filePath,thisr.getServicesListPath(),"csid",false, thisr);

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
	

	protected JSONObject miniViewAbstract(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,JSONObject out, String servicepath, String filePath) throws UnderlyingStorageException{
		try{
			//actually use cache
			String cachelistitem = "/"+servicepath;
			if(filePath !=null){
				cachelistitem = cachelistitem+"/"+filePath;
			}

			if(!cachelistitem.startsWith("/")){
				cachelistitem = "/"+cachelistitem;
			}
			String dnName = getDisplayNameKey();
			String g1=getGleanedValue(cache,cachelistitem,"refName");
			String g2=getGleanedValue(cache,cachelistitem,"shortIdentifier");
			String g3=getGleanedValue(cache,cachelistitem,dnName);
			String g4=getGleanedValue(cache,cachelistitem,"csid");
			String g5=getGleanedValue(cache,cachelistitem,"termStatus");
			String g6=getGleanedValue(cache,cachelistitem,"workflow");
			if(g1==null|| g2==null||g3==null||g4==null||g5==null){
				if(log.isWarnEnabled()) {
					StringBuilder sb = new StringBuilder();
					sb.append("ConfiguredVocabStorage fanning out ");
					if(g2!=null) {
						sb.append("(shId:");
						sb.append(g2);
						sb.append(")");
					}
					if(g4!=null) {
						sb.append("(csid:");
						sb.append(g4);
						sb.append(")");
					}
					sb.append(", as could not get: ");
					if(g1==null)
						sb.append("refName,");
					if(g2==null)
						sb.append("shortIdentifier,");
					if(g3==null)
						sb.append("dnName,");
					if(g4==null)
						sb.append("csid,");
					if(g5==null)
						sb.append("termStatus,");
					log.warn(sb.toString());
				}
				JSONObject cached =  get(storage, creds,cache,servicepath,filePath);
				g1 = cached.getString("refid");
				g2 = cached.getString("shortIdentifier");
				g3 = cached.getString(dnName);
				g4 = cached.getString("csid");
				g5 = cached.getString("termStatus");
				
			}
			out.put(dnName, g3);
			out.put("refid", g1);
			out.put("csid", g4);
			out.put("termStatus", g5);
			out.put("workflow", g6);
			//out.put("authorityid", cached.get("authorityid"));
			out.put("shortIdentifier", g2);
			out.put("recordtype",r.getWebURL());
			RefName.AuthorityItem item = RefName.AuthorityItem.parse(g1); 
			out.put("namespace",item.getParentShortIdentifier());
			
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (ExistException e) {
			throw new UnderlyingStorageException("ExistException exception"+e.getLocalizedMessage(),e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSONException exception"+e.getLocalizedMessage(),e);
		}
	}

}
