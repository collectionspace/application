/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.relation;

import java.util.Map;

import org.collectionspace.chain.util.xtmpl.XTmplDocument;
import org.dom4j.Document;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;

class Relation {
	private String src,dst,src_type,dst_type,type,metatype,id;
	private RelationFactory factory;
	
	Relation(RelationFactory factory,String id,Document in) throws JaxenException {
		this.factory=factory;
		fromDocument(id,in);
	}
	Relation(RelationFactory factory, String id, String src_type, String src,
			String type, String dst_type, String dst) {
		this(factory, id, src_type, src,
			type, "", dst_type, dst);
	}
	Relation(RelationFactory factory, String id, String src_type, String src,
			String type, String metatype, String dst_type, String dst) {
		this.src_type=src_type;
		this.src=src;
		this.type=type;
		this.metatype=metatype;
		this.dst_type=dst_type;
		this.dst=dst;
		this.factory=factory;
		this.id=id;
	}

	public Document toDocument() {
		XTmplDocument doc=factory.getTemplate().makeDocument();
		doc.setText("src-type",src_type);
		doc.setText("src",src);
		doc.setText("type",type);
		doc.setText("metatype",metatype);
		doc.setText("dst-type",dst_type);
		doc.setText("dst",dst);
		return doc.getDocument();
	}
	
	public void fromDocument(String id,Document in) throws JaxenException {
		Map<String,Dom4jXPath> deplate=factory.getDeplate();
		src_type=deplate.get("src-type").stringValueOf(in);
		dst_type=deplate.get("dst-type").stringValueOf(in);
		src=deplate.get("src").stringValueOf(in);
		dst=deplate.get("dst").stringValueOf(in);
		type=deplate.get("type").stringValueOf(in);
		metatype=deplate.get("metatype").stringValueOf(in);
		this.id=id;
	}
	
	public String getID() { return id; }
	public String getSourceType() { return src_type; }
	public String getDestinationType() { return dst_type; }
	public String getSourceId() { return src; }
	public String getDestinationId() { return dst; }
	public String getRelationshipType() { return type; }
	public String getRelationshipMetaType() { return metatype; }
}
