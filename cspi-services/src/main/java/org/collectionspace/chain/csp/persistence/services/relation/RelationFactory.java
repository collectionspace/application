/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.relation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.chain.util.xtmpl.XTmplTmpl;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;

public class RelationFactory {
	private XTmplTmpl template;
	private Map<String,Dom4jXPath> deplate=new HashMap<String,Dom4jXPath>();

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
	
	private Map<String,String> getProperties(String file) throws IOException {
		Properties p=new Properties();
		p.load(getResource(file));
		Map<String,String> out=new HashMap<String,String>();
		for(Entry<Object, Object> e : p.entrySet()) {
			out.put((String)e.getKey(),(String)e.getValue());
		}
		return out;
	}
	
	public RelationFactory() throws InvalidXTmplException, DocumentException, JaxenException, IOException {
		template=XTmplTmpl.compile(getDocument("relation.xtmpl"));
		// Load deplate
		for(Map.Entry<String,String> e : getProperties("relation.xpath").entrySet()) {
			deplate.put(e.getKey(),new Dom4jXPath(e.getValue()));
		}
	}
	
	public Relation create(String id,String src_type,String src,String type,String dst_type,String dst) {
		return new Relation(this,id,src_type,src,type,dst_type,dst);
	}
	
	public Relation load(String id,Document doc) throws JaxenException {
		return new Relation(this,id,doc);
	}
	
	XTmplTmpl getTemplate() { return template; }
	Map<String,Dom4jXPath> getDeplate() { return deplate; }
}
