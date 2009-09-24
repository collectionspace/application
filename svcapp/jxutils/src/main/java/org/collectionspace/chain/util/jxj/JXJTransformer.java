/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jxj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.util.jpath.InvalidJPathException;
import org.collectionspace.chain.util.jpath.JPathPath;
import org.collectionspace.chain.util.jtmpl.InvalidJTmplException;
import org.collectionspace.chain.util.jtmpl.JTmplDocument;
import org.collectionspace.chain.util.jtmpl.JTmplTmpl;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.chain.util.xtmpl.XTmplDocument;
import org.collectionspace.chain.util.xtmpl.XTmplTmpl;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Transforms JSON into XML and vice versa. Derived from JXJFile.
 * 
 */
public class JXJTransformer {
	private static class PathAndMap { 
		private Dom4jXPath path; 
		private Map<String,String> map=new HashMap<String,String>();
	}

	private XTmplTmpl jxtmpl;
	private Map<String,JPathPath> jxattach=new HashMap<String,JPathPath>();
	private HashMap<String, Map<String, JPathPath>> jxmapattach=new HashMap<String,Map<String,JPathPath>>();
	private JTmplTmpl xjtmpl;
	private Map<String,Dom4jXPath> xjattach=new HashMap<String,Dom4jXPath>();
	private Set<PathAndMap> xjmapattach=new HashSet<PathAndMap>();	

	private Set<String> xjmultiple=new HashSet<String>();
	private Set<String> jxmultiple=new HashSet<String>();

	private String addMappingTag(Map<String,JPathPath> map,Node n,String key,String value) throws InvalidJXJException, InvalidJPathException {
		String n_key=((Element)n).attributeValue(key);
		String n_json=((Element)n).attributeValue(value);
		if(n_key==null)
			throw new InvalidJXJException("mapping tag requires key attribute");
		if(n_json==null)
			throw new InvalidJXJException("mapping tag requires json attribute");
		map.put(n_key,JPathPath.compile(n_json));
		return n_key;
	}

	@SuppressWarnings("unchecked")
	JXJTransformer(String key,Node document) throws InvalidJXJException {
		try {
			// json2xml
			Node jxtmpl_node=document.selectSingleNode("json2xml/template/*");
			if(jxtmpl_node==null)
				throw new InvalidJXJException("Missing json2xml/template tag");
			DocumentFactory df=new DocumentFactory();
			Document jxtmpl_doc=df.createDocument();
			jxtmpl_doc.setRootElement((Element)jxtmpl_node.clone());
			jxtmpl=XTmplTmpl.compile(jxtmpl_doc);
			for(Node n : (List<Node>)document.selectNodes("json2xml/map/mapping")) {
				String n_key=addMappingTag(jxattach,n,"key","json");
				if("yes".equals(((Element)n).attributeValue("multiple")))
					jxmultiple.add(n_key);
			}
			for(Node n : (List<Node>)document.selectNodes("json2xml/map/assembly")) {
				String n_key=((Element)n).attributeValue("key");
				Map<String,JPathPath> inner_paths=new HashMap<String,JPathPath>();
				if(n_key==null)
					throw new InvalidJXJException("assembly tag requires key attribute");
				for(Node m : (List<Node>)n.selectNodes("mapping")) {
					addMappingTag(inner_paths,m,"assembly-key","json");
				}
				jxmapattach.put(n_key,inner_paths);
			}
			// xml2json
			JSONObject template=new JSONObject(document.selectSingleNode("xml2json/template").getText());
			xjtmpl=JTmplTmpl.compile(template);
			for(Node n : (List<Node>)document.selectNodes("xml2json/map/mapping")) {
				String n_key=((Element)n).attributeValue("key");
				String n_xpath=((Element)n).attributeValue("xpath");
				if(n_key==null)
					throw new InvalidJXJException("mapping tag requires key attribute");
				if(n_xpath==null)
					throw new InvalidJXJException("mapping tag requires xpath attribute");
				xjattach.put(n_key,new Dom4jXPath(n_xpath));
				if("yes".equals(((Element)n).attributeValue("multiple")))
					xjmultiple.add(n_key);
			}
			for(Node n : (List<Node>)document.selectNodes("xml2json/map/assembly")) {
				PathAndMap pam=new PathAndMap();
				pam.path=new Dom4jXPath(((Element)n).attributeValue("xpath"));
				if(pam.path==null)
					throw new InvalidJXJException("mapping tag requires xpath attribute");				
				for(Node m : (List<Node>)n.selectNodes("mapping")) {
					String m_key=((Element)m).attributeValue("key");
					String m_akey=((Element)m).attributeValue("assembly-key");
					if(m_key==null)
						throw new InvalidJXJException("mapping tag requires key attribute");
					if(m_akey==null)
						throw new InvalidJXJException("mapping tag requires attribute-key attribute");
					pam.map.put(m_akey,m_key);
				}
				xjmapattach.add(pam);
			}
		} catch (InvalidXTmplException e) {
			throw new InvalidJXJException("Invalid XML template in JXJ key="+key,e);
		} catch (InvalidJPathException e) {
			throw new InvalidJXJException("Invalid JPath in JXJ key="+key,e);
		} catch (JSONException e) {
			throw new InvalidJXJException("Bad JSON key="+key,e);
		} catch (InvalidJTmplException e) {
			throw new InvalidJXJException("Invalid JTmpl key="+key,e);
		} catch (JaxenException e) {
			throw new InvalidJXJException("Invalid XPath key="+key,e);
		}
	}

	private void setDocumentKey(XTmplDocument doc,String key,Object value) throws JSONException, InvalidXTmplException {
		if(jxmultiple.contains(key)) {
			if(value==null)
				value=new JSONArray();
			int len=((JSONArray)value).length();
			String[] out=new String[len];
			for(int i=0;i<len;i++) {
				Object v=((JSONArray)value).get(i);
				if(!(v instanceof String))
					throw new InvalidXTmplException("Array value is not a string");
				out[i]=(String)v;
			}
			doc.setTexts(key,out);
		}
		else {
			if(value==null)
				value="";
			doc.setText(key,(String)value);	
		}
	}

	// Just JXJ
	public Document json2xml(JSONObject json) throws InvalidXTmplException {
		XTmplDocument doc=jxtmpl.makeDocument();
		// Regular mapping
		try {
			for(Map.Entry<String,JPathPath> entry : jxattach.entrySet()) {
				setDocumentKey(doc,entry.getKey(),entry.getValue().get(json));
			}
			// Map mappings
			for(Map.Entry<String,Map<String,JPathPath>> e : jxmapattach.entrySet()) {
				JSONObject map=new JSONObject();
				for(Map.Entry<String,JPathPath> f : e.getValue().entrySet())
					map.put(f.getKey(),f.getValue().get(json));
				doc.setText(e.getKey(),map.toString());
			}
			return doc.getDocument();
		} catch (JSONException e) {
			throw new InvalidXTmplException("Invalid JSON key",e);
		}
	}

	// XXX just JXJ
	@SuppressWarnings("unchecked")
	public JSONObject xml2json(Document xml) throws InvalidJTmplException, InvalidJXJException {
		JTmplDocument doc=xjtmpl.makeDocument();
		try {
			// Regular mapping
			for(Map.Entry<String,Dom4jXPath> entry : xjattach.entrySet()) {
				if(xjmultiple.contains(entry.getKey())) {
					List<Node> nodes=entry.getValue().selectNodes(xml.getDocument());
					JSONArray a=new JSONArray();
					for(Node n : nodes) {
						a.put(n.getText());
					}
					doc.set(entry.getKey(),a);
				} else {
					Node n=((Node)entry.getValue().selectSingleNode(xml.getDocument()));
					String text="";
					if(n!=null) {
						text=n.getText();
					}
					doc.set(entry.getKey(),text);
				}
			}
			// Map mappings
			for(PathAndMap pam : xjmapattach) {
				Node n=((Node)pam.path.selectSingleNode(xml.getDocument()));
				JSONObject map=new JSONObject();
				if(n!=null && !"".equals(n)) {
					try {
						map=new JSONObject(n.getText());
					} catch (JSONException e1) {} // Leave empty
				}
				for(Map.Entry<String,String> e : pam.map.entrySet()) {
					try {
						doc.set(e.getValue(),map.getString(e.getKey()));
					} catch (JSONException e1) {} // Leave unset
				}
			}
		} catch (JaxenException e) {
			throw new InvalidJXJException("Invalid XPath",e);
		}
		return doc.getJSON();
	}
}
