package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfiguredVocabStorage implements ContextualisedStorage {
	private static final Logger log=LoggerFactory.getLogger(ConfiguredVocabStorage.class);
	private ServicesConnection conn;
	private VocabInstanceCache vocab_cache;
	private URNProcessor urn_processor;
	private Record r;

	public ConfiguredVocabStorage(Record r,ServicesConnection conn) throws  DocumentException {
		this.conn=conn;
		urn_processor=new URNProcessor(r.getURNSyntax());
		Map<String,String> vocabs=new ConcurrentHashMap<String,String>();
		for(Instance n : r.getAllInstances()) {
			vocabs.put(n.getTitleRef(),n.getTitle());
		}
		vocab_cache=new VocabInstanceCache(r,conn,vocabs);
		this.r=r;
	}

	private Document createEntry(String section,String namespace,String root_tag,JSONObject data,String vocab,String refname) throws UnderlyingStorageException, ConnectionException, ExistException, JSONException {
		Document out=XmlJsonConversion.convertToXml(r,data,section);
		Element root=out.getRootElement();
		Element vocabtag=root.addElement(r.getInTag());
		vocabtag.addText(vocab);
		Element refnametag=root.addElement("refName");
		if(refname!=null)
			refnametag.addText(refname);
		if(r.isType("compute-displayname")) {
			Element dnc=root.addElement("displayNameComputed");
			dnc.addText("false");
		}
		log.info("createEntry() ::: "+out.asXML());
		return out;
	}

	private String getDisplayNameKey() throws UnderlyingStorageException {
		Field dnf=r.getDisplayNameField();
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
				body.put(record_path[0],createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,null));
			}	
			// First send without refid (don't know csid)	
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/"+r.getServicesURL()+"/"+vocab+"/items",body,creds,cache);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
			// This time with refid
			String csid=out.getURLTail();
			String refname=urn_processor.constructURN("id",vocab,"id",out.getURLTail(),name);
			body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				body.put(record_path[0],createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,refname));
			}
			ReturnedMultipartDocument out2=conn.getMultipartXMLDocument(RequestMethod.PUT,"/"+r.getServicesURL()+"/"+vocab+"/items/"+csid,body,creds,cache);
			if(out2.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());			
			cache.setCached(getClass(),new String[]{"namefor",vocab,out.getURLTail()},name);
			cache.setCached(getClass(),new String[]{"reffor",vocab,out.getURLTail()},refname);
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath,JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create at named path");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			String vocab=vocab_cache.getVocabularyId(creds,cache,filePath.split("/")[0]);
			int status=conn.getNone(RequestMethod.DELETE,generateURL(vocab,filePath.split("/")[1]),null,creds,cache);
			if(status>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+status);
			cache.removeCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]});
			cache.removeCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]});
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		}	
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			List<String> out=new ArrayList<String>();
			String vocab=vocab_cache.getVocabularyId(creds,cache,rootPath);
			String url="/"+r.getServicesURL()+"/"+vocab+"/items";
			String prefix=null;
			if(restrictions!=null && restrictions.has(getDisplayNameKey()))
				prefix=restrictions.getString(getDisplayNameKey());
			// XXX pagination support
			url+="?pgSz=10000";
			if(prefix!=null)
				url+="&pt="+URLEncoder.encode(prefix,"UTF8");
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,url,null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve vocabularies");
			String[] tag_parts=r.getServicesListPath().split(",",2);
			List<Node> objects=doc.getDocument().selectNodes(tag_parts[1]);
			for(Node object : objects) {
				String name=object.selectSingleNode("displayName").getText();
				String csid=object.selectSingleNode("csid").getText();
				if(prefix==null || name.toLowerCase().contains(prefix.toLowerCase()))
					out.add(csid);
				cache.setCached(getClass(),new String[]{"namefor",vocab,csid},name);
				String refname=urn_processor.constructURN("id",vocab,"id",csid,name);
				cache.setCached(getClass(),new String[]{"reffor",vocab,csid},refname);
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Error parsing JSON");
		}
	}

	private String generateURL(String vocab,String path) throws ExistException, ConnectionException, UnderlyingStorageException {
		return r.getServicesURL()+"/"+vocab+"/items/"+path;
	}

	private JSONObject urnGet(String vocab,String entry,String refname) throws JSONException, ExistException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		out.put("recordtype",r.getWebURL());
		out.put("refid",refname);
		out.put("csid",entry);
		out.put("displayName",urn_processor.deconstructURN(refname,false)[5]);
		return out;
	}
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] path=filePath.split("/");
			String vocab,csid;
			if("_direct".equals(path[0])) {
				if("urn".equals(path[1])) {
					return urnGet(path[2],path[3],path[4]);
				}
				vocab=path[2];
				csid=path[3];
			} else {
				vocab=vocab_cache.getVocabularyId(creds,cache,path[0]);
				csid=path[1];	
			}			
			/*
			String name=(String)cache.getCached(getClass(),new String[]{"namefor",vocab,csid});
			String refid=(String)cache.getCached(getClass(),new String[]{"reffor",vocab,csid});
			*/
			// XXX actually use cache			
			JSONObject out=get(creds,cache,vocab,csid);
			cache.setCached(getClass(),new String[]{"namefor",vocab,csid},out.get(getDisplayNameKey()));
			cache.setCached(getClass(),new String[]{"reffor",vocab,csid},out.get("refid"));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot generate JSON",e);
		}
	}

	private JSONObject get(CSPRequestCredentials creds,CSPRequestCache cache,String vocab,String csid) throws ConnectionException, ExistException, UnderlyingStorageException, JSONException {
		// XXX pagination support
		ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,generateURL(vocab,csid),null,creds,cache);
		if(doc.getStatus()==404)
			throw new ExistException("Does not exist");
		if(doc.getStatus()>299)
			throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus());
		JSONObject out=new JSONObject();
		String name=null;
		String refid=null;
		for(String section : r.getServicesRecordPaths()) {
			String path=r.getServicesRecordPath(section);
			String[] record_path=path.split(":",2);
			String[] tag_path=record_path[1].split(",",2);
			Document result=doc.getDocument(record_path[0]);
			if("common".equals(section)) { // XXX hardwired :(
				name=result.selectSingleNode(tag_path[1]+"/displayName").getText();
				refid=result.selectSingleNode(tag_path[1]+"/refName").getText();
			}
			XmlJsonConversion.convertToJson(out,r,result);				
		}
		out.put(getDisplayNameKey(),name);
		out.put("csid",csid);
		out.put("refid",refid);
		out.put("recordtype",r.getWebURL());
		return out;
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String name=jsonObject.getString(getDisplayNameKey());
			String vocab=vocab_cache.getVocabularyId(creds,cache,filePath.split("/")[0]);
			String refname=urn_processor.constructURN("id",vocab,"id",filePath.split("/")[1],name);
			Map<String,Document> body=new HashMap<String,Document>();
			for(String section : r.getServicesRecordPaths()) {
				String path=r.getServicesRecordPath(section);
				String[] record_path=path.split(":",2);
				String[] tag_path=record_path[1].split(",",2);
				body.put(record_path[0],createEntry(section,tag_path[0],tag_path[1],jsonObject,vocab,refname));
			}
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,generateURL(vocab,filePath.split("/")[1]),body,creds,cache);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
			cache.setCached(getClass(),new String[]{"namefor",vocab,filePath.split("/")[1]},name);
			cache.setCached(getClass(),new String[]{"reffor",vocab,filePath.split("/")[1]},refname);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}
}
