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

public class JXJTransformer {
	private XTmplTmpl jxtmpl;
	private Map<String,JPathPath> jxattach=new HashMap<String,JPathPath>();
	private JTmplTmpl xjtmpl;
	private Map<String,Dom4jXPath> xjattach=new HashMap<String,Dom4jXPath>();
	private Set<String> xjmultiple=new HashSet<String>();
	
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
				String n_key=((Element)n).attributeValue("key");
				String n_json=((Element)n).attributeValue("json");
				if(n_key==null)
					throw new InvalidJXJException("mapping tag requires key attribute");
				if(n_json==null)
					throw new InvalidJXJException("mapping tag requires json attribute");
				jxattach.put(n_key,JPathPath.compile(n_json));
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
		if(value instanceof String)
			doc.setText(key,(String)value);	
		else if(value instanceof JSONArray) {
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
	}
	
	// Just JXJ
	public Document json2xml(JSONObject json) throws InvalidXTmplException {
		XTmplDocument doc=jxtmpl.makeDocument();
		for(Map.Entry<String,JPathPath> entry : jxattach.entrySet()) {
			try {
				setDocumentKey(doc,entry.getKey(),entry.getValue().get(json));
			} catch (JSONException e) {
				throw new InvalidXTmplException("Invalid JSON key="+entry.getKey(),e);
			}
		}
		return doc.getDocument();
	}
	
	// XXX just JXJ
	@SuppressWarnings("unchecked")
	public JSONObject xml2json(Document xml) throws InvalidJTmplException, InvalidJXJException {
		JTmplDocument doc=xjtmpl.makeDocument();
		for(Map.Entry<String,Dom4jXPath> entry : xjattach.entrySet()) {
			try {
				if(xjmultiple.contains(entry.getKey())) {
					List<Node> nodes=entry.getValue().selectNodes(xml.getDocument());
					JSONArray a=new JSONArray();
					for(Node n : nodes) {
						a.put(n.getText());
					}
					doc.set(entry.getKey(),a);
				} else {
					String text=((Node)entry.getValue().selectSingleNode(xml.getDocument())).getText();
					doc.set(entry.getKey(),text);
				}
			} catch (JaxenException e) {
				throw new InvalidJXJException("Invalid XPath",e);
			}
		}
		return doc.getJSON();
	}
}
