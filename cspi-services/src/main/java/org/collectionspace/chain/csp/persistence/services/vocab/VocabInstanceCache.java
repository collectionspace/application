/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabInstanceCache {
	private static final Logger log=LoggerFactory.getLogger(VocabInstanceCache.class);
	private Map<String,String> csids=new HashMap<String,String>();
	private ServicesConnection conn;
	private Map<String,String> vocabs;
	private Record r;
	
	VocabInstanceCache(Record r,ServicesConnection conn,Map<String,String> vocabs) {
		this.conn=conn;
		this.vocabs=vocabs;
		this.r=r;
	}
	

	private String vocabByShortIdentifier(String name) throws ExistException {
		if(!vocabs.containsKey(name))
			throw new ExistException("No such vocab "+name);
		return vocabs.get(name);
	}
	
	private Document createList(String namespace,String tag,String id, String vocab_type) throws ExistException {
		Document out=DocumentFactory.getInstance().createDocument();
		String[] path=tag.split("/");
		Element root=out.addElement("ns2:"+path[0],namespace);
		for(int i=1;i<path.length;i++) {
			root=root.addElement(path[i]);
		}
		Element nametag=root.addElement("displayName");
		nametag.addText(vocabByShortIdentifier(id));

		Element sidtag=root.addElement("shortIdentifier");
		sidtag.addText(id);
		
		Element vocabtag=root.addElement("vocabType");
		vocabtag.addText(vocab_type); 
	//	log.info(out.asXML());
		return out;
	}
	
	// Only called if doesn't exist
	private synchronized void createVocabulary(CSPRequestCredentials creds,CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
		Map<String,Document> body=new HashMap<String,Document>();
		String[] path_parts=r.getServicesSingleInstancePath().split(":",2);
		String vocab_type = r.getVocabType();
		String[] tag_parts=path_parts[1].split(",",2);
		body.put(path_parts[0],createList(tag_parts[0],tag_parts[1],id,vocab_type));
		ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/"+r.getServicesURL()+"/",body,creds,cache);
	//	log.info("/"+r.getServicesURL()+"/");
		if(out.getStatus()>299)
			throw new UnderlyingStorageException("Could not create vocabulary status="+out.getStatus(),
					out.getStatus(), "/"+r.getServicesURL()+"/");
		csids.put(id,out.getURLTail());
	}
	
	//XXX pagination? argh
	@SuppressWarnings("unchecked")
	private void buildVocabularies(CSPRequestCredentials creds,CSPRequestCache cache) throws ConnectionException, UnderlyingStorageException {
		ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/"+r.getServicesURL()+"/",null,creds,cache);
		Document doc=data.getDocument();
		if(doc==null)
			throw new UnderlyingStorageException("Could not retrieve vocabularies");		
		String[] path_parts=r.getServicesInstancesPath().split(":",2);
		String[] tag_parts=path_parts[1].split(",",2);
		List<Node> objects=doc.getDocument().selectNodes(tag_parts[1]);
		for(Node object : objects) {
			String name = "MISSING";
			if(null !=object.selectSingleNode("displayName") ){
				name=object.selectSingleNode("displayName").getText();
			}
			if(null == object.selectSingleNode("shortIdentifier")){
				continue;
			}
			String base=object.selectSingleNode("shortIdentifier").getText();			
			if(base==null)
				continue;
			if(!vocabs.containsKey(base)){
				vocabs.put(base, name);
			}
			csids.put(base,object.selectSingleNode("csid").getText());
		}
	}
	
	String getVocabularyId(CSPRequestCredentials creds,CSPRequestCache cache,String id) throws ConnectionException, UnderlyingStorageException, ExistException {
//must allow for the dynamic creation of instances when the system is working
		vocabs=new ConcurrentHashMap<String,String>();
		for(Instance n : r.getAllInstances()) {
			vocabs.put(n.getTitleRef(),n.getTitle());
		}
		if(csids.containsKey(id))
			return csids.get(id);
		synchronized(getClass()) {
			buildVocabularies(creds,cache);
			if(csids.containsKey(id))
				return csids.get(id);
			createVocabulary(creds,cache,id);
			if(csids.containsKey(id))
				return csids.get(id);
			throw new UnderlyingStorageException("Bad vocabulary "+id);
		}
	}
}
