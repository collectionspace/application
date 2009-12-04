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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		cspace_264_hack=new ServicesIdentifierMap(conn,
				"collectionobjects",
				"collectionobjects-common-list/collection-object-list-item",
				"collectionobjects_common/objectNumber","collectionobjects_common");
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
			accnum = "";
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
		for(Node n : (List<Node>)(in.selectNodes("collectionobjects_common/otherNumber"))) {
			out.append(n.getText());
			out.append(' ');
			n.detach();
		}
		if(!"".equals(out.toString())) {
			Node n=((Element)in.selectSingleNode("collectionobjects_common")).addElement("otherNumber");
			n.setText(out.toString());
			in.selectNodes("collectionobjects_common").add(n);
		}
		return in;
	}

	private Document cspace266Hack_unmunge(Document in) {
		Node n=in.selectSingleNode("collectionobjects_common/otherNumber");
		if(n==null)
			return in;
		String value=n.getText();
		n.detach();
		if(value==null || "".equals(value))
			return in;
		for(String v : value.split(" ")) {
			Node nw=((Element)in.selectSingleNode("collectionobjects_common")).addElement("otherNumber");
			nw.setText(v);
		}
		return in;
	}

	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		// XXX Here's what we do because of CSPACE-264
		autocreateJSON(filePath,jsonObject);
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
			List<Node> objects=all.getDocument().selectNodes("collectionobjects-common-list/collection-object-list-item");
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
		return doc==null || doc.selectNodes("collectionobjects_common/*").size()==0;
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException, UnderlyingStorageException {
		try {
			// XXX Here's what we do because of CSPACE-264
			// 1. Check this isn't a genuine CSID (via autocreate): rely on guids not clashing with museum IDs
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath,null);
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument("collectionobjects_common"))) {
				return jxj.xml2json(cspace266Hack_unmunge(doc.getDocument("collectionobjects_common")));
			}
			boolean blasted=false;
			boolean exhausted=false;
			while(!exhausted) {
				// 2. Assume museum ID
				String csid=cspace_264_hack.getCSID("_path:"+filePath);
				if(csid==null) {
					exhausted=true;
					break;
				}
				doc = conn.getMultipartXMLDocument(RequestMethod.GET,"collectionobjects/"+csid,null);
				// XXX End of here's what we do because of CSPACE-264		
				// vv This is what we should do
				// ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath);
				if(doc.getStatus()==404 || cspace268Hack_empty(doc.getDocument("collectionobjects_common"))) {
					if(!blasted) {
						cspace_264_hack.blastCache();
						exhausted=false;
						blasted=true;
					} else
						exhausted=true;
				} else
					break;
			}
			if(exhausted)
				throw new ExistException("Does not exist "+filePath);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
			// XXX Here's what we do because of CSPACE-264
			return cspace264Hack_unmunge(jxj.xml2json(cspace266Hack_unmunge(doc.getDocument("collectionobjects_common"))));
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
			jsonObject=cspace264Hack_munge(jsonObject,filePath);
			Document data=cspace266Hack_munge(jxj.json2xml(jsonObject));
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath,null);
			String csid=null;
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument("collectionobjects_common"))) {
				csid=filePath;
			} else {
				csid=cspace_264_hack.getCSID("_path:"+filePath);
			}
			Map<String,Document> parts2=new HashMap<String,Document>();
			parts2.put("collectionobjects_common",data);			
			doc = conn.getMultipartXMLDocument(RequestMethod.PUT,"collectionobjects/"+csid,parts2);
			if(doc.getStatus()==404 || cspace268Hack_empty(doc.getDocument("collectionobjects_common")))
				throw new ExistException("Not found: collectionobjects/"+csid);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException, UnimplementedException {
		try {
			String accnum="";
			if(jsonObject.has("accessionNumber"))
				accnum=jsonObject.getString("accessionNumber");
			if(!"".equals(filePath))
				accnum=filePath;
			Document data=cspace266Hack_munge(jxj.json2xml(jsonObject));
			Map<String,Document> parts1=new HashMap<String,Document>();
			parts1.put("collectionobjects_common",data);
			ReturnedURL url = conn.getMultipartURL(RequestMethod.POST,"collectionobjects/",parts1);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			String csid=url.getURLTail();
			data=cspace266Hack_munge(jxj.json2xml(cspace264Hack_munge(jsonObject,accnum)));
			System.err.println(data.asXML());
			Map<String,Document> parts2=new HashMap<String,Document>();
			parts2.put("collectionobjects_common",data);
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.PUT,"collectionobjects/"+csid,parts2);
			// XXX Set path
			if(doc.getStatus()==404 || cspace268Hack_empty(doc.getDocument("collectionobjects_common")))
				throw new ExistException("Not found: collectionobjects/"+csid);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
			return csid;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnimplementedException("Error in template",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Cannot munge data",e);
		}
	}

	public void deleteJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			ReturnedMultipartDocument doc = conn.getMultipartXMLDocument(RequestMethod.GET,"collectionobjects/"+filePath,null);
			String csid=null;
			if((doc.getStatus()>199 && doc.getStatus()<300) && !cspace268Hack_empty(doc.getDocument("collectionobjects_common"))) {
				csid=filePath;
			} else {
				cspace_264_hack.blastCache();
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
