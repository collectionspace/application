package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;

public class VocabInstanceCache {
	private Map<String,String> csids=new HashMap<String,String>();
	private ServicesConnection conn;
	private String prefix,section,list_item_path,tag,namespace;
	private Map<String,String> vocabs;
	
	VocabInstanceCache(ServicesConnection conn,String section,String prefix,String list_item_path,Map<String,String> vocabs,String namespace,String tag) {
		this.conn=conn;
		this.section=section;
		this.prefix=prefix;
		this.list_item_path=list_item_path;
		this.vocabs=vocabs;
		this.namespace=namespace;
		this.tag=tag;
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
	
	private String confound(String name) throws ExistException {
		if(!vocabs.containsKey(name))
			throw new ExistException("No such vocab "+name);
		return vocabs.get(name)+" ("+name+")";
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
	
	String getVocabularyId(CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
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
}
