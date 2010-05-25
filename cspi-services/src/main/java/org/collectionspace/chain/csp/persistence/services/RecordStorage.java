package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.collectionspace.chain.csp.persistence.services.vocab.URNProcessor;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
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

public class RecordStorage implements ContextualisedStorage {
	private static final Logger log=LoggerFactory.getLogger(RecordStorage.class);
	private ServicesConnection conn;
	private Record r;
	private Map<String,String> view_good=new HashMap<String,String>();
	private Map<String,String> view_map=new HashMap<String,String>();
	private Set<String> xxx_view_deurn=new HashSet<String>();

	public RecordStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		this.conn=conn;
		this.r=r;
		// Number
		if(r.getMiniNumber()!=null){
			view_good.put(r.getMiniNumber().getID(),"number");
			view_map.put(r.getMiniNumber().getServicesTag(),r.getMiniNumber().getID());
			if(r.getMiniNumber().getAutocompleteInstance()!=null)
				xxx_view_deurn.add(r.getMiniNumber().getID());
		}
		// Summary
		if(r.getMiniSummary() !=null){
			view_good.put(r.getMiniSummary().getID(),"summary");
			view_map.put(r.getMiniSummary().getServicesTag(),r.getMiniSummary().getID());
			if(r.getMiniSummary().getAutocompleteInstance()!=null)
				xxx_view_deurn.add(r.getMiniSummary().getID());
		}
	}

	private void setGleanedValue(CSPRequestCache cache,String path,String key,String value) {
		cache.setCached(getClass(),new String[]{"glean",path,key},value);
	}

	private String getGleanedValue(CSPRequestCache cache,String path,String key) {
		return (String)cache.getCached(getClass(),new String[]{"glean",path,key});
	}

	private void convertToJson(JSONObject out,Document in) throws JSONException {
		XmlJsonConversion.convertToJson(out,r,in);
	}

	/**
	 * Prepare the request for storage and send it through to the service layer
	 */
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {

			//XXX CSPACE-1828 hack
			if(r.getID().equals("role")){
				String name = jsonObject.get("roleName").toString();
				jsonObject.put("roleName","ROLE_"+name);
			}
			Map<String,Document> parts=new HashMap<String,Document>();
			Document doc = null;
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				doc=XmlJsonConversion.convertToXml(r,jsonObject,section);
				parts.put(record_path[0],doc);
			}
			ReturnedURL url;
			//log.info(doc.asXML());
			//some records are accepted as multipart in the service layers, others arent, that's why we split up here
			if(r.isMultipart())
				url = conn.getMultipartURL(RequestMethod.POST,r.getServicesURL()+"/",parts,creds,cache);
			else
				url = conn.getURL(RequestMethod.POST, r.getServicesURL()+"/", doc, creds, cache);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot post to full path");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			int status=conn.getNone(RequestMethod.DELETE,r.getServicesURL()+"/"+filePath,null,creds,cache);
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}		
	}

	private String xxx_deurn(String in) throws UnderlyingStorageException {
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

	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document list=null;
			JSONObject out = new JSONObject();
			JSONObject pagination = new JSONObject();
			List<String> listitems=new ArrayList<String>();
			String postfix = "?";
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
			}
			postfix = postfix.substring(0, postfix.length()-1);
			if(postfix.length() == 0){postfix +="/";}
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,r.getServicesURL()+postfix,null,creds,cache);
			if(all.getStatus()!=200){
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			}
			list=all.getDocument();
			
			
			List<Node> nodes=list.selectNodes("/"+r.getServicesListPath().split("/")[0]+"/*");
			if(r.getServicesListPath().equals("roles_list/*") || r.getServicesListPath().equals("permissions_list/*")){
				//XXX hack to deal with roles being inconsistent
				// XXX CSPACE-1887 workaround
				for(Node node : nodes) {
					if(node.matches("/"+r.getServicesListPath())){
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
					if(node.matches("/"+r.getServicesListPath())){
						List<Node> fields=node.selectNodes("*");
						String csid=node.selectSingleNode("csid").getText();
						for(Node field : fields) {
							if("csid".equals(field.getName())) {
								int idx=csid.lastIndexOf("/");
								if(idx!=-1)
									csid=csid.substring(idx+1);
								listitems.add(csid);
							} else if("uri".equals(field.getName())) {
							// Skip!
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
					}
					else{
						pagination.put(node.getName(), node.getText());
					}
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

	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document list=null;
			List<String> out=new ArrayList<String>();
			String postfix = "?";
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
			}
			postfix = postfix.substring(0, postfix.length()-1);
			if(postfix.length() == 0){postfix +="/";}
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,r.getServicesURL()+postfix,null,creds,cache);
			if(all.getStatus()!=200){
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			}
			list=all.getDocument();
			
			List<Node> objects=list.selectNodes(r.getServicesListPath());
			if(r.getServicesListPath().equals("roles_list/*")){
				//XXX hack to deal with roles being inconsistent
				// XXX CSPACE-1887 workaround
				for(Node object : objects) {
					String csid = object.valueOf( "@csid" );
					out.add(csid);					
				}
				
			}
			else{
				for(Node object : objects) {
					List<Node> fields=object.selectNodes("*");
					String csid=object.selectSingleNode("csid").getText();
					for(Node field : fields) {
						if("csid".equals(field.getName())) {
							int idx=csid.lastIndexOf("/");
							if(idx!=-1)
								csid=csid.substring(idx+1);
							out.add(csid);						
						} else if("uri".equals(field.getName())) {
							// Skip!
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
				}
			}
			//log.info(out.toString());
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=filePath.split("/",2);
			if(parts.length==2) {
				return viewRetrieveJSON(root,creds,cache,parts[0],parts[1]);
			} else
				return simpleRetrieveJSON(creds,cache,filePath);
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		}
	}

	public JSONObject viewRetrieveJSON(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		if("view".equals(view))
			return miniViewRetrieveJSON(cache,creds,filePath);
		else if("refs".equals(view))
			return refViewRetrieveJSON(storage,creds,cache,filePath);
		else
			return new JSONObject();
	}

	// XXX support URNs for reference
	private JSONObject miniForURI(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String refname,String uri) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		return storage.retrieveJSON(storage,creds,cache,"direct/urn/"+uri+"/"+refname);
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
					String fieldName = key.split(":")[1];
					Field fieldinstance = (Field)r.getField(fieldName);
					
					if(uri!=null && uri.startsWith("/"))
						uri=uri.substring(1);
					JSONObject data=miniForURI(storage,creds,cache,refname,uri);
					data.put("sourceFieldselector", fieldinstance.getSelector());
					data.put("sourceFieldName", fieldName);
					data.put("sourceFieldType", r.getID());
					out.put(key,data);
				}
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection problem",e);
		}
	}

	public JSONObject miniViewRetrieveJSON(CSPRequestCache cache,CSPRequestCredentials creds,String filePath) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=new JSONObject();
		Set<String> to_get=new HashSet<String>(view_good.keySet());
		// Try to fullfil from gleaned info
		for(String good : view_good.keySet()) {
			String gleaned=getGleanedValue(cache,r.getServicesURL()+"/"+filePath,good);
			if(gleaned==null)
				continue;
			if(xxx_view_deurn.contains(good))
				gleaned=xxx_deurn(gleaned);
			out.put(view_good.get(good),gleaned);
			to_get.remove(good);
		}
		// Do a full request
		if(to_get.size()>0) {
			JSONObject data=simpleRetrieveJSON(creds,cache,filePath);
			for(String good : to_get) {
				if(data.has(good)) {
					String vkey=view_good.get(good);
					String value=data.getString(good);
					if(xxx_view_deurn.contains(good))
						value=xxx_deurn(value);
					out.put(vkey,value);
				}
			}
		}
		return out;
	}

	public JSONObject simpleRetrieveJSON(CSPRequestCredentials creds,CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out=new JSONObject();
			if(r.isMultipart()){
				ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,r.getServicesURL()+"/"+filePath,null,creds,cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new ExistException("Does not exist "+filePath);
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] parts=path.split(":",2);
					convertToJson(out,doc.getDocument(parts[0]));
				}
			}else{
				ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, r.getServicesURL()+"/"+filePath,null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new ExistException("Does not exist "+filePath);
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] parts=path.split(":",2);
					convertToJson(out,doc.getDocument());
				}
			}
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject jsonObject)
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
