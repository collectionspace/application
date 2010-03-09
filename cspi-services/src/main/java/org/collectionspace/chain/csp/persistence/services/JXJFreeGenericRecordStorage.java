package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.util.jtmpl.InvalidJTmplException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.chain.util.jxj.JXJFile;
import org.collectionspace.chain.util.jxj.JXJTransformer;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class JXJFreeGenericRecordStorage implements ContextualisedStorage {
	private ServicesConnection conn;
	private String prefix,part,items;
	private Record r;
	private Map<String,String> view_good=new HashMap<String,String>();
	private Map<String,String> view_map=new HashMap<String,String>();
	private Set<String> xxx_view_deurn=new HashSet<String>();
	
	protected JXJFreeGenericRecordStorage() {}
	
	protected void init(ServicesConnection conn,Record r,String prefix,String part,String items,
			String[] mini_key,String[] mini_xml,String[] mini_value,boolean[] xxx_mini_deurn) throws InvalidJXJException, DocumentException, IOException {	
		this.prefix=prefix;
		this.part=part;
		this.items=items;
		this.conn=conn;
		if(mini_value==null)
			mini_value=mini_key;
		if(mini_key.length!=mini_value.length || mini_key.length!=mini_xml.length)
			throw new IOException("key, map value arrays must be same length"); // XXX should be another
		for(int i=0;i<mini_key.length;i++)
			view_good.put(mini_key[i],mini_value[i]);
		for(int i=0;i<mini_key.length;i++)
			view_map.put(mini_xml[i],mini_key[i]);
		for(int i=0;i<xxx_mini_deurn.length;i++)
			if(xxx_mini_deurn[i])
				xxx_view_deurn.add(mini_key[i]);
		this.r=r;
	}
	
	private void setGleanedValue(CSPRequestCache cache,String path,String key,String value) {
		cache.setCached(getClass(),new String[]{"glean",path,key},value);
	}

	private String getGleanedValue(CSPRequestCache cache,String path,String key) {
		return (String)cache.getCached(getClass(),new String[]{"glean",path,key});
	}
	
	private JSONObject convertToJson(Document in) throws InvalidJTmplException, InvalidJXJException, JSONException {
		return XmlJsonConversion.convertToJson(r,in);
		//return jxj.xml2json(in);
	}
	
	public String autocreateJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document doc=XmlJsonConversion.convertToXml(r,jsonObject);
			System.err.println(doc.asXML());
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(part,doc);
			ReturnedURL url = conn.getMultipartURL(RequestMethod.POST,prefix+"/",parts);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
	}

	public void createJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot post to full path");
	}

	public void deleteJSON(CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			int status=conn.getNone(RequestMethod.DELETE,prefix+"/"+filePath,null);
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
	public String[] getPaths(CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document list=null;
			List<String> out=new ArrayList<String>();
			if(restrictions!=null && restrictions.has("keywords")) {
				/* Keyword search */
				String data=URLEncoder.encode(restrictions.getString("keywords"),"UTF-8");
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,prefix+"/search?keywords="+data,null);
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200");
				list=all.getDocument();
			} else {
				/* Full list */
				ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,prefix+"/",null);
				if(all.getStatus()!=200)
					throw new ConnectionException("Bad request during identifier cache map update: status not 200");
				list=all.getDocument();
			}
			List<Node> objects=list.selectNodes(items);
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
							setGleanedValue(cache,prefix+"/"+csid,json_name,value);
						}
					}
				}

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

	public JSONObject retrieveJSON(CSPRequestCache cache,String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=filePath.split("/",2);
			if(parts.length==2)
				return viewRetrieveJSON(cache,parts[0],parts[1]);
			else
				return simpleRetrieveJSON(filePath);
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		}
	}

	public JSONObject viewRetrieveJSON(CSPRequestCache cache,String filePath,String view) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject out=new JSONObject();
		Set<String> to_get=new HashSet<String>(view_good.keySet());
		// Try to fullfil from gleaned info
		for(String good : view_good.keySet()) {
			String gleaned=getGleanedValue(cache,prefix+"/"+filePath,good);
			if(gleaned==null)
				continue;
			if(xxx_view_deurn.contains(good))
				gleaned=xxx_deurn(gleaned);
			out.put(view_good.get(good),gleaned);
			to_get.remove(good);
		}
		// Do a full request
		if(to_get.size()>0) {
			JSONObject data=simpleRetrieveJSON(filePath);
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

	public JSONObject simpleRetrieveJSON(String filePath) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,prefix+"/"+filePath,null);
			if((doc.getStatus()>199 && doc.getStatus()<300)) {
				return convertToJson(doc.getDocument(part));
			}
			throw new ExistException("Does not exist "+filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJXJException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public void updateJSON(CSPRequestCache cache,String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document data=XmlJsonConversion.convertToXml(r,jsonObject);
			Map<String,Document> parts=new HashMap<String,Document>();
			parts.put(part,data);
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.PUT,prefix+"/"+filePath,parts);
			if(doc.getStatus()==404)
				throw new ExistException("Not found: "+prefix+"/"+filePath);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
			
		}
	}
}
