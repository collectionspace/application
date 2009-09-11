/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.xtmpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

/** A template instance which can be filled in with values. Once done, you can then retrieve the result.
 * 
 */
public class XTmplDocument {
	private Map<String,String> attach;
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
	
	@SuppressWarnings("unchecked")
	public void setTexts(String key,String[] texts) {
		Node basis=document.selectSingleNode(attach.get(key));
		Element parent=basis.getParent();
		int pos=parent.indexOf(basis);
		List<Node> nodes=(List<Node>)parent.content();
		List<Node> after=new ArrayList(nodes.subList(pos+1,nodes.size()));	
		basis.detach();
		for(Node n : after)
			n.detach();
		for(String text : texts) {
			Node renewed=(Node)basis.clone();
			renewed.setText(text);	
			parent.add(renewed);
		}
		for(Node n : after)
			parent.add(n);
	}
	
	public Document getDocument() {
		return document;
	}
}
