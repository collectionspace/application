/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.util.jtmpl.InvalidJTmplException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.chain.util.jxj.JXJFile;
import org.collectionspace.chain.util.jxj.JXJTransformer;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is a bag of hacks and tricks re Services/API integration. As they get fixed, this class should shrink.
 * 
 * And spite of Pride, in erring Reason's spite,
 * One truth is clear, Whatever is, is right. 
 *                             - Alexander Pope
 */

/**
 * ServicesCollectionObjectStorage implements storage for collection object, and will 
 * probably be supplemented by similar classes for other services. However, as we add these, common factors will emerge
 * which will allow them to be refactored into a common base class (or similar), meaning that 
 * ServicesCollectionObjectStorage and similar just end up with special code for a particular interface.
 */
class ServicesCollectionObjectStorage implements Storage {
	private ServicesConnection conn;
	private JXJTransformer jxj;
	private ServicesIdentifierMap cspace_264_hack;
	private static Logger log=LoggerFactory.getLogger(ServicesCollectionObjectStorage.class);
	
	// XXX refactor into util
	private Document getDocument(String in) throws DocumentException, IOException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(stream);
		stream.close();
		return doc;
	}
	
	public ServicesCollectionObjectStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		this.conn=conn;
		cspace_264_hack=new ServicesIdentifierMap(conn);
		JXJFile jxj_file=JXJFile.compile(getDocument("collectionobject.jxj"));
		jxj=jxj_file.getTransformer("collection-object");
		if(jxj==null)
			throw new InvalidJXJException("Missing collection-object transform.");
	}

	private JSONObject cspace264Hack_unmunge(JSONObject in) {
		// 1. Extract accession number from other numbers
		try {
			JSONArray ons=in.getJSONArray("objectNumber");
			String accnum=null;
			JSONArray new_ons=new JSONArray();
			for(int i=0;i<ons.length();i++) {
				String v=ons.getString(i);
				if(v.startsWith("_accnum:")) {
					accnum=v.substring("_accnum:".length());
				} else {
					new_ons.put(v);
				}
			}
			in.remove("objectNumber");
			in.put("objectNumber",new_ons);
			if(accnum!=null) {
				in.remove("accessionNumber");
				in.put("accessionNumber",accnum);
			}
		} catch (JSONException e) {}
		return in;
	}
	
	private JSONObject cspace264Hack_munge(JSONObject in,String path) {
		// 1. Copy accession number into other number
		JSONArray ons=new JSONArray();
		String accnum;
		try {
			accnum = in.getString("accessionNumber");
		} catch (JSONException e1) {
			log.error("CSPACE-264 hack workaround failed: no accessionNumber in object");
			return in;
		}		
		try {
			ons=in.getJSONArray("objectNumber");
		} catch (JSONException e) {}
		ons.put("_accnum:"+accnum);
		in.remove("objectNumber");
		try {
			in.put("objectNumber",ons);
		} catch (JSONException e) {
			log.error("CSPACE-264 hack workaround failed: json exception writing objectNumber",e);			
		}
		// 2. Copy path into accession number
		in.remove("accessionNumber");
		try {
			in.put("accessionNumber","_path:"+path);
		} catch (JSONException e) {
			log.error("CSPACE-264 hack workaround failed: json exception writing accessionNumber",e);
		}
		return in;
	}
		
	// XXX
	@SuppressWarnings("unchecked")
	private Document cspace266Hack_munge(Document in) {
		StringBuffer out=new StringBuffer();
		for(Node n : (List<Node>)(in.selectNodes("collection-object/otherNumber"))) {
			out.append(n.getText());
			out.append(' ');
			n.detach();
		}
		Node n=((Element)in.selectSingleNode("collection-object")).addElement("otherNumber");
		n.setText(out.toString());
		in.selectNodes("collection-object").add(n);
		return in;
	}

	private Document cspace266Hack_unmunge(Document in) {
		Node n=in.selectSingleNode("collection-object/otherNumber");
		if(n==null)
			return in;
		String value=n.getText();
		n.detach();
		if(value==null || "".equals(value))
			return in;
		for(String v : value.split(" ")) {
			Node nw=((Element)in.selectSingleNode("collection-object")).addElement("otherNumber");
			nw.setText(v);
		}
		return in;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		// XXX Here's what we do because of CSPACE-264
		jsonObject=cspace264Hack_munge(jsonObject,filePath);
		autocreateJSON("",jsonObject);
		// XXX End of here's what we do because of CSPACE-264		
		// Here's what we should do ->
		// throw new UnimplementedException("Cannot create collectionobject at known path, use autocreateJSON");
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(String subdir) throws UnderlyingStorageException {
		try {
			List<String> out=new ArrayList<String>();
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/");			
			if(all.getStatus()!=200)
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			List<Node> objects=all.getDocument().selectNodes("collection-object-list/collection-object-list-item");
			for(Node object : objects) {
				String csid=object.selectSingleNode("csid").getText();
				int idx=csid.lastIndexOf("/");
				if(idx!=-1)
					csid=csid.substring(idx+1);
				String mid=cspace_264_hack.fromCSID(csid);
				if(mid.startsWith("_path:")) {
					out.add(mid.substring("_path:".length()));
				} else {
					out.add(csid);
				}
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	private boolean cspace268Hack_empty(Document doc) {
		return doc.selectNodes("collection-object/*").size()==0;
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException, UnderlyingStorageException {
		try {
			// XXX Here's what we do because of CSPACE-264
			// 1. Check this isn't a genuine CSID (via autocreate): rely on guids not clashing with museum IDs
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath);
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument())) {
				return jxj.xml2json(cspace266Hack_unmunge(doc.getDocument()));
			}
			// 2. Assume museum ID
			String csid=cspace_264_hack.getCSID("_path:"+filePath);
			doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+csid);
			// XXX End of here's what we do because of CSPACE-264		
			// vv This is what we should do
			// ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath);
			if(doc.getStatus()==404 || cspace268Hack_empty(doc.getDocument())) {
				throw new ExistException("Does not exist "+filePath);
			}
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
			// XXX Here's what we do because of CSPACE-264
			return cspace264Hack_unmunge(jxj.xml2json(cspace266Hack_unmunge(doc.getDocument())));
			// XXX End of here's what we do because of CSPACE-264		
			// vv This is what we should do
			// return jxj.xml2json(doc.getDocument());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJXJException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException {
		try {
			Document data=cspace266Hack_munge(jxj.json2xml(jsonObject));
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath);
			String csid=null;
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument())) {
				csid=filePath;
			} else {
				csid=cspace_264_hack.getCSID("_path:"+filePath);
			}
			doc = conn.getXMLDocument(RequestMethod.PUT,"collectionobjects/"+csid,data);
			if(doc.getStatus()==404 || cspace268Hack_empty(doc.getDocument()))
				throw new ExistException("Not found: collecitonobjects/"+csid);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	// XXX cannot test until CSPACE-264 is fixed.
	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException, UnimplementedException {
		try {
			Document doc=cspace266Hack_munge(jxj.json2xml(jsonObject));
			ReturnedURL url = conn.getURL(RequestMethod.POST,"collectionobjects/",doc);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnimplementedException("Error in template",e);
		}
	}

	public void deleteJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath);
			String csid=null;
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument())) {
				csid=filePath;
			} else {
				csid=cspace_264_hack.getCSID("_path:"+filePath);
			}
			int status=conn.getNone(RequestMethod.DELETE,"collectionobjects/"+csid,null);
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}		
	}
}
