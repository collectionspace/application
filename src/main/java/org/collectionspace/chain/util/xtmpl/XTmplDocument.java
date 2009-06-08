package org.collectionspace.chain.util.xtmpl;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

public class XTmplDocument {
	private Map<String,String> attach=new HashMap<String,String>();
	private Document document;

	XTmplDocument(Document document,Map<String,String> attach) {
		this.document=(Document)document.clone();
		this.attach=attach;
	}
	
	public void setText(String key,String text) {
		document.selectSingleNode(attach.get(key)).setText(text);
	}
	
	public void setContents(String key,Element value) {
		Element e=(Element)document.selectSingleNode(attach.get(key));
		e.add(value);
	}
	
	public Document getDocument() {
		return document;
	}
}
