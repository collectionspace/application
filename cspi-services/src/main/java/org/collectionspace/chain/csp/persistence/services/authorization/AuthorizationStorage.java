package org.collectionspace.chain.csp.persistence.services.authorization;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(AuthorizationStorage.class);
	private PermissionCache permissions;
	
	public AuthorizationStorage(Record r, ServicesConnection conn) throws DocumentException, IOException{
		super(r,conn);
		initializeGlean(r);
		Record permissionRecord = r.getSpec().getRecord("permission");
		this.permissions = new PermissionCache(permissionRecord,conn);
	}




	/**
	 * Gets the csid from an role or account out of the json authorization
	 * @param data
	 * @param primaryField
	 * @return
	 * @throws JSONException
	 */
	private String getSubCsid(JSONObject data, String primaryField) throws JSONException{
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
	public String autocreateJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			ReturnedURL url = null;
			Document doc = null;

			//used by userroles and permroles as they have complex urls
			if(r.hasPrimaryField()){
				//XXX test if works: need to delete first before create/update
			//	deleteJSON(root,creds,cache,filePath);

				for(String section : r.getServicesRecordPaths()) {
					doc=XmlJsonConversion.convertToXml(r,jsonObject,section);
					String path = r.getServicesURL();
					path = path.replace("*", getSubCsid(jsonObject,r.getPrimaryField()));

					deleteJSON(root,creds,cache,path);
					log.info(doc.asXML());
					url = conn.getURL(RequestMethod.POST, path, doc, creds, cache);		
					
				}
			}
			else{

				Map<String,Document> parts=new HashMap<String,Document>();
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					doc=XmlJsonConversion.convertToXml(r,jsonObject,section);
					parts.put(record_path[0],doc);
				}
				//some records are accepted as multipart in the service layers, others arent, that's why we split up here
				if(r.isMultipart())
					url = conn.getMultipartURL(RequestMethod.POST,r.getServicesURL()+"/",parts,creds,cache);
				else
					url = conn.getURL(RequestMethod.POST, r.getServicesURL()+"/", doc, creds, cache);
				if(url.getStatus()>299 || url.getStatus()<200)
					throw new UnderlyingStorageException("Bad response "+url.getStatus());
			}
			
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}
	
	public void createJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String filePath, JSONObject jsonObject) 
	throws ExistException,UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot post to full path");
	}

	/**
	 * Remove an object in the Service Layer.
	 */
	public void deleteJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			int status = 0;
			if(r.hasDeleteMethod()){
				//get all data to post back
				//hopefully the filepath we have is the filepath to get the data from
				Document doc = null;
				try{
					doc = simpleRetrieveXML(creds,cache,filePath+"/");
					String nodepath = "/"+ r.getServicesListPath()+"/*";
					List<Node> nodes=doc.selectNodes(nodepath);
					if(nodes.size()>0){
						status=201;
						status=conn.getNone(RequestMethod.DELETE,r.getServicesURL()+"/"+filePath,null,creds,cache);	
						//post it back to delete it
	/*
						log.info("TRYING TO DELETE ACCOUNTROLE");
						log.info(doc.asXML());
						log.info(filePath+"?_method=delete");
						ReturnedURL url = null;
						url = conn.getURL(RequestMethod.POST, filePath+"?_method=delete", doc, creds, cache);
						status = url.getStatus();
						//status=conn.getNone(RequestMethod.POST,r.getServicesURL()+"/"+filePath+"?_method=delete",doc,creds,cache);
						 	
	*/
					}
					else{//if we didn't need to delete it because there was nothing there then everything was good
						status=201;
					}
				}
				catch(ExistException ex){
					//ignore as nothign to delete if nothing exists	
					status=201;
				}
				
			}
			else{
				status=conn.getNone(RequestMethod.DELETE,r.getServicesURL()+"/"+filePath,null,creds,cache);				
			}
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}



	/**
	 * Returns a list of csid's from a certain type of record
	 */
	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String rootPath, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document list=null;
			List<String> out=new ArrayList<String>();
			String postfix = "?";
			if(restrictions!=null){
				if(restrictions.has("keywords")) {
					/* Keyword search */
					String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
					postfix += "res="+data+"&";
				} 
				if(restrictions.has("pageSize")){
					postfix += "pgSz="+restrictions.getString("pageSize")+"&";
				}
				if(restrictions.has("pageNum")){
					postfix += "pgNum="+restrictions.getString("pageNum")+"&";
				}
			}
			postfix = postfix.substring(0, postfix.length()-1);
			if(postfix.length() == 0){postfix +="/";}
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,r.getServicesURL()+postfix,null,creds,cache);
			if(all.getStatus()!=200){
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			}
			list=all.getDocument();
			List<Node> objects=list.selectNodes(r.getServicesListPath());

			for(Node object : objects) {
				String csid = object.valueOf( "@csid" );
				out.add(csid);
				setGleanedValue(cache,r.getServicesURL()+"/"+csid,view_map.get(object.getName()),object.getText());
			}

			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	/**
	 * Gets a list of csids of a certain type of record together with the pagination info
	 * permissions might need to break the mold tho.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String rootPath, JSONObject restrictions) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out = new JSONObject();

			
			Document list=null;
			String prefix=null;
			Boolean queryadded = false;
			JSONObject pagination = new JSONObject();
			List<String> listitems=new ArrayList<String>();
			String postfix = "?";
			if(restrictions!=null){
				if(restrictions.has("keywords")) {
					/* Keyword search */
					String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
						postfix += r.getServicesSearchKeyword()+"="+data+"&";
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
			postfix = postfix.substring(0, postfix.length()-1);
			if(postfix.length() == 0){postfix +="/";}
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,r.getServicesURL()+postfix,null,creds,cache);
			if(all.getStatus()!=200){
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			}
			list=all.getDocument();
			
			List<Node> nodes=list.selectNodes("/"+r.getServicesListPath().split("/")[0]+"/*");
			for(Node node : nodes) {
				if(node.matches("/"+r.getServicesListPath())){
					String csid = node.valueOf( "@csid" );
					listitems.add(csid);
					if(view_map.get(node.getName())!=null) {
						setGleanedValue(cache,r.getServicesURL()+"/"+csid,view_map.get(node.getName()),node.getText());
					}
				}
				else{
					pagination.put(node.getName(), node.getText());
				}
			}
			
			out.put("pagination", pagination);
			out.put("listItems", listitems.toArray(new String[0]));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	
	public JSONObject retrieveJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {

			String[] parts = filePath.split("/");
			
			if(parts.length > 2){
				Spec s = r.getSpec();
				//{csid}/userrole/{csid}
				if(s.hasRecordByWebUrl(parts[1])){
					String path = s.getRecordByWebUrl(parts[1]).getServicesURL();
					int len = parts.length -1 ;
					for(int i=0; i<len;i++){
						path = path.replace("*", parts[i]);
						i++;
					}
					filePath = path + "/" + parts[len];
					return simpleRetrieveJSONFullPath(creds,cache,filePath,s.getRecordByWebUrl(parts[1]).getRecord());
				}
				else{
					//{csid}/refobj/bob
					String extra = "";
					if(parts.length==3){
						extra = parts[2];
					}
					return viewRetrieveJSON(root,creds,cache,parts[0],parts[1],extra);
				} 
			}
			else {
				return simpleRetrieveJSON(creds,cache,filePath);
			}
			
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		}
	}
	
	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view,String extra) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		if("view".equals(view))
			return miniViewRetrieveJSON(cache,creds,filePath,extra);
		else if("refs".equals(view))
			return refViewRetrieveJSON(storage,creds,cache,filePath);
		else
			return new JSONObject();
	}



	public JSONObject refViewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		try {
			JSONObject out=new JSONObject();
			//not all the records need a reference, look in default.xml for which that don't
			if(r.hasTermsUsed()){
				String path = r.getServicesURL()+"/"+filePath+"/authorityrefs";
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200");
				Document list=all.getDocument();
				for(Object node : list.selectNodes("authority-ref-list/authority-ref-item")) {
					if(!(node instanceof Element))
						continue;
					String key=((Element)node).selectSingleNode("sourceField").getText();
					String uri=((Element)node).selectSingleNode("uri").getText();
					String refname=((Element)node).selectSingleNode("refName").getText();
					if(uri!=null && uri.startsWith("/"))
						uri=uri.substring(1);
					JSONObject data=miniForURI(storage,creds,cache,refname,uri);
					out.put(key,data);
				}
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection problem",e);
		}
	}

	public JSONObject miniViewRetrieveJSON(CSPRequestCache cache,CSPRequestCredentials creds,String filePath, String extra) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
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
			if(!name.startsWith(summarylistname)&& !name.equals("summary") && !name.equals("number")){
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
					gleaned=getGleanedValue(cache,r.getServicesURL()+"/"+filePath,id);
					if(gleaned!=null){
						//if find value stop
						break;
					}
				}
			}
			else{
				gleaned=getGleanedValue(cache,r.getServicesURL()+"/"+filePath,good);
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
		// Do a full request
		if(to_get.size()>0) {
			JSONObject data=simpleRetrieveJSON(creds,cache,filePath);
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
						if(value!=null){
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
	
	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		String fullpath = r.getServicesURL()+"/"+filePath;
		return simpleRetrieveJSONFullPath( creds, cache, fullpath);
	}
	
	private Document simpleRetrieveXML(CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws UnimplementedException, ExistException, ConnectionException {
		if(r.isMultipart()){
			throw new UnimplementedException("this functionality is currently only for non multipart xml");
		}else{
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, filePath,null, creds, cache);
			if((doc.getStatus()<200 || doc.getStatus()>=300))
				throw new ExistException("Does not exist "+filePath);

			return doc.getDocument();
		}
	}
	public JSONObject simpleRetrieveJSONFullPath(CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		return simpleRetrieveJSONFullPath( creds, cache, filePath, r);
	}
	
	public JSONObject simpleRetrieveJSONFullPath(CSPRequestCredentials creds,CSPRequestCache cache,String filePath, Record thisr) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out=new JSONObject();
			if(r.isMultipart()){
				ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,filePath,null,creds,cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new ExistException("Does not exist "+filePath);
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] parts=path.split(":",2);
					convertToJson(out,doc.getDocument(parts[0]));
				}
			}else{
				ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, filePath,null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new ExistException("Does not exist "+filePath);

				convertToJson(out,doc.getDocument(),thisr);
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	@Override
	public void updateJSON(ContextualisedStorage root, CSPRequestCredentials creds, CSPRequestCache cache, String filePath, JSONObject jsonObject) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Map<String,Document> parts=new HashMap<String,Document>();
			Document doc = null;
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				doc=XmlJsonConversion.convertToXml(r,jsonObject,section);
				parts.put(record_path[0],doc);
			}
			int status = 0;
			if(r.isMultipart()){
				ReturnedMultipartDocument docm = conn.getMultipartXMLDocument(RequestMethod.PUT,r.getServicesURL()+"/"+filePath,parts,creds,cache);
				status = docm.getStatus();
			}
			else{ 
				ReturnedDocument docm = conn.getXMLDocument(RequestMethod.PUT, r.getServicesURL()+"/"+filePath, doc, creds, cache);
				status = docm.getStatus();
			}
			
			if(status==404)
				throw new ExistException("Not found: "+r.getServicesURL()+"/"+filePath);
			if(status>299 || status<200)
				throw new UnderlyingStorageException("Bad response "+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);

		}
	}

}
