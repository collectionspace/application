package org.collectionspace.chain.csp.persistence.services.relation;

import java.util.Map;

import org.collectionspace.chain.util.xtmpl.XTmplDocument;
import org.dom4j.Document;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;

class Relation {
	private String src,dst,src_type,dst_type,type;
	private RelationFactory factory;
	
	Relation(RelationFactory factory,Document in) throws JaxenException {
		this.factory=factory;
		fromDocument(in);
	}
	Relation(RelationFactory factory,String src_type,String src,String type,String dst_type,String dst) {
		this.src_type=src_type;
		this.src=src;
		this.type=type;
		this.dst_type=dst_type;
		this.dst=dst;
		this.factory=factory;
	}
	
	public Document toDocument() {
		XTmplDocument doc=factory.getTemplate().makeDocument();
		doc.setText("src-type",src_type);
		doc.setText("src",src);
		doc.setText("type",type);
		doc.setText("dst-type",dst_type);
		doc.setText("dst",dst);
		return doc.getDocument();
	}
	
	public void fromDocument(Document in) throws JaxenException {
		Map<String,Dom4jXPath> deplate=factory.getDeplate();
		src_type=deplate.get("src-type").stringValueOf(in);
		dst_type=deplate.get("dst-type").stringValueOf(in);
		src=deplate.get("src").stringValueOf(in);
		dst=deplate.get("dst").stringValueOf(in);
		type=deplate.get("type").stringValueOf(in);
	}
	
	public String getSourceType() { return src_type; }
	public String getDestinationType() { return dst_type; }
	public String getSourceId() { return src; }
	public String getDestinationId() { return dst; }
	public String getRelationshipType() { return type; }
}
