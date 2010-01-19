package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.GenericRecordStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.chain.util.xtmpl.XTmplDocument;
import org.collectionspace.chain.util.xtmpl.XTmplTmpl;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.json.JSONException;
import org.json.JSONObject;

public class GenericVocabStorage implements ContextualisedStorage {
	private ServicesConnection conn;
	private Map<String,String> csids=new HashMap<String,String>();

	private Map<String,String> vocabs;
	private Pattern urn_syntax;
	private String prefix,section,list_item_path,items_section,item_path,name_path,urn_builder,namespace,tag,in_tag;
	
	public GenericVocabStorage(ServicesConnection conn,String urn_builder,Pattern urn_regexp,Map<String,String> vocabs,
			String namespace,String prefix,String section,String items_section,String list_item_path,String item_path,
			String name_path,String tag,String in_tag) throws InvalidXTmplException, DocumentException {
		this.conn=conn;
		urn_syntax=urn_regexp;
		this.urn_builder=urn_builder;
		this.vocabs=new ConcurrentHashMap<String,String>(vocabs);
		this.prefix=prefix;
		this.section=section;
		this.items_section=items_section;
		this.list_item_path=list_item_path;
		this.item_path=item_path;
		this.name_path=name_path;
		this.namespace=namespace;
		this.tag=tag;
		this.in_tag=in_tag;
	}

	// XXX refactor
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}

	// XXX refactor
	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}
	
	private String confound(String name) throws ExistException {
		if(!vocabs.containsKey(name))
			throw new ExistException("No such vocab "+name);
		return vocabs.get(name)+" ("+name+")";
	}
	
	private String unconfound(String name) {
		if(name==null)
			return null;
		if(!name.endsWith(")"))
			return null;
		int pos=name.lastIndexOf('(');
		if(pos==-1)
			return null;
		String rest=name.substring(pos+1);
		return rest.substring(0,rest.length()-1);
	}
		
	// Only called if doesn't exist
	private synchronized void createVocabulary(CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
		Map<String,Document> body=new HashMap<String,Document>();
		body.put(section,createList(id));
		ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/"+prefix+"/",body);
		if(out.getStatus()>299)
			throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
		csids.put(id,out.getURLTail());
	}

	@SuppressWarnings("unchecked")
	private void buildVocabularies(CSPRequestCache cache) throws ConnectionException, UnderlyingStorageException {
		ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/"+prefix+"/",null);
		Document doc=data.getDocument();
		if(doc==null)
			throw new UnderlyingStorageException("Could not retrieve vocabularies");
		List<Node> objects=doc.getDocument().selectNodes(list_item_path);
		for(Node object : objects) {
			String name=object.selectSingleNode("displayName").getText();
			String base=unconfound(name);
			if(base==null)
				continue;
			if(!vocabs.containsKey(base))
				continue;
			csids.put(base,object.selectSingleNode("csid").getText());
		}
	}
	
	private String getVocabularyId(CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
		if(csids.containsKey(id))
			return csids.get(id);
		synchronized(getClass()) {
			buildVocabularies(cache);
			if(csids.containsKey(id))
				return csids.get(id);
			createVocabulary(cache,id);
			if(csids.containsKey(id))
				return csids.get(id);
			throw new UnderlyingStorageException("Bad vocabulary "+id);
		}
	}
	
	private String constructURN(CSPRequestCache cache,String vocab_id,String entry_id,String display) throws UnderlyingStorageException, ConnectionException, ExistException {
		try {
			String out=urn_builder;
			out=out.replaceAll("\\{vocab\\}",URLEncoder.encode(vocab_id,"UTF-8"));
			out=out.replaceAll("\\{entry\\}",URLEncoder.encode(entry_id,"UTF-8"));
			out=out.replaceAll("\\{display\\}",URLEncoder.encode(display,"UTF-8"));
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		}
	}
	
	private String[] deconstructURN(CSPRequestCache cache,String urn) throws ExistException {
		Matcher m=urn_syntax.matcher(urn);
		if(!m.matches())
			throw new ExistException("Bad URN, does not exist");
		return new String[]{m.group(1),m.group(2),m.group(3),m.group(4)};
	}
	
	private Document createEntry(String name,String vocab) {
		Document out=DocumentFactory.getInstance().createDocument();
		Element root=out.addElement("ns2:"+items_section,namespace);
		Element nametag=root.addElement("displayName");
		nametag.addText(name);
		Element vocabtag=root.addElement(in_tag);
		vocabtag.addText(vocab);
		return out;
	}
	
	private Document createList(String id) throws ExistException {
		Document out=DocumentFactory.getInstance().createDocument();
		Element root=out.addElement("ns2:"+tag,namespace);
		Element nametag=root.addElement("displayName");
		nametag.addText(confound(id));
		Element vocabtag=root.addElement("vocabType");
		vocabtag.addText("enum");
		return out;
	}
	
	public String autocreateJSON(CSPRequestCache cache,String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
				if(!jsonObject.has("name"))
					throw new UnderlyingStorageException("Missing name argument to data");
				String name=jsonObject.getString("name");
				String vocab=getVocabularyId(cache,filePath);
				Map<String,Document> body=new HashMap<String,Document>();
				body.put(items_section,createEntry(name,vocab));
				ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/"+prefix+"/"+vocab+"/items",body);
				if(out.getStatus()>299)
					throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
				String urn=constructURN(cache,vocab,out.getURLTail(),name);
				cache.setCached(getClass(),new String[]{"namefor",out.getURLTail()},name);
				return urn;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}

	public void createJSON(CSPRequestCache cache, String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create at named path");
	}

	public void deleteJSON(CSPRequestCache cache, String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			int status=conn.getNone(RequestMethod.DELETE,URNtoURL(cache,filePath),null);
			if(status>299)
				throw new UnderlyingStorageException("Could not retrieve vocabulary status="+status);
			cache.removeCached(getClass(),new String[]{"namefor",deconstructURN(cache,filePath)[2]});
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		}	
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(CSPRequestCache cache,String rootPath,JSONObject restrictions)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			List<String> out=new ArrayList<String>();
			String vocab=getVocabularyId(cache,rootPath);
			String url="/"+prefix+"/"+vocab+"/items";
			String prefix=null;
			if(restrictions!=null && restrictions.has("name"))
				prefix=restrictions.getString("name");
			// XXX pagination support
			url+="?pgSz=10000";
			if(prefix!=null)
				url+="&pt="+URLEncoder.encode(prefix,"UTF8");
			ReturnedDocument data = conn.getXMLDocument(RequestMethod.GET,url,null);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve vocabularies");
			List<Node> objects=doc.getDocument().selectNodes(item_path);
			for(Node object : objects) {
				String name=object.selectSingleNode("displayName").getText();
				String csid=object.selectSingleNode("csid").getText();
				if(prefix==null || name.toLowerCase().contains(prefix.toLowerCase()))
					out.add(constructURN(cache,vocab,csid,name));
				cache.setCached(getClass(),new String[]{"namefor",csid},name);
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

	private String URNtoURL(CSPRequestCache cache,String path) throws ExistException, ConnectionException, UnderlyingStorageException {
		String[] parts=deconstructURN(cache,path);
		String vocab=getVocabularyId(cache,parts[0]);
		if(!vocab.equals(parts[1]))
			throw new ExistException("Not in this vocabulary");
		return "/"+prefix+"/"+parts[1]+"/items/"+parts[2];
	}

	private String URNNewName(CSPRequestCache cache,String path,String name) throws ExistException, ConnectionException, UnderlyingStorageException {
		String[] parts=deconstructURN(cache,path);
		String vocab=getVocabularyId(cache,parts[0]);
		if(!vocab.equals(parts[1]))
			throw new ExistException("Not in this vocabulary");
		return constructURN(cache,vocab,parts[2],name);
	}
	
	private String URNtoVocab(CSPRequestCache cache,String path) throws ExistException, ConnectionException, UnderlyingStorageException {
		String[] parts=deconstructURN(cache,path);
		return getVocabularyId(cache,parts[0]);
	}
		
	public JSONObject retrieveJSON(CSPRequestCache cache, String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {			
			String name=(String)cache.getCached(getClass(),new String[]{"namefor",deconstructURN(cache,filePath)[2]});
			if(name==null) {			
				// XXX pagination support
				ReturnedMultipartDocument doc=conn.getMultipartXMLDocument(RequestMethod.GET,URNtoURL(cache,filePath),null);
				if(doc.getStatus()==404)
					throw new ExistException("Does not exist");
				if(doc.getStatus()>299)
					throw new UnderlyingStorageException("Could not retrieve vocabulary status="+doc.getStatus());
				name=doc.getDocument(items_section).selectSingleNode(name_path).getText();
			}
			JSONObject out=new JSONObject();
			out.put("name",name);
			out.put("csid",URNNewName(cache,filePath,name));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot generate JSON",e);
		}
	}

	public void updateJSON(CSPRequestCache cache,String filePath,JSONObject jsonObject)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			if(!jsonObject.has("name"))
				throw new UnderlyingStorageException("Missing name argument to data");
			String name=jsonObject.getString("name");
			Map<String,Document> body=new HashMap<String,Document>();
			body.put(items_section,createEntry(name,URNtoVocab(cache,filePath)));
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,URNtoURL(cache,filePath),body);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus());
			cache.setCached(getClass(),new String[]{"namefor",deconstructURN(cache,filePath)[2]},name);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Connection exception",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot parse surrounding JSON",e);
		}
	}
}
