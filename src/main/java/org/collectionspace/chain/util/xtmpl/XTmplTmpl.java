package org.collectionspace.chain.util.xtmpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;

public class XTmplTmpl {	
	private static final String XTMPL_URI="http://www.collectionspace.org/xtmpl";
	
	private Map<String,String> attach=new HashMap<String,String>();
	private Document document;
	
	public static XTmplTmpl compile(Document template) throws InvalidXTmplException {
		return new XTmplTmpl(template);
	}
	
	private XTmplTmpl(Document template) throws InvalidXTmplException {
		compileTemplate(template);
	}

	@SuppressWarnings("unchecked")
	private void removeNamespaces(Element e) {
		for(Namespace  ns : (List<Namespace>)e.declaredNamespaces()) {
			if(!XTMPL_URI.equals(ns.getURI()))
				continue;
			e.remove(ns);
		}
		for(Element k : (List<Element>)e.elements()) {
			removeNamespaces(k);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void compileTemplate(Document template) throws InvalidXTmplException {
		try {
			Map<String,String> map = new HashMap<String,String>();
			map.put( "xtmpl",XTMPL_URI);
			Dom4jXPath xpath = new Dom4jXPath("//.[@xtmpl:point]");
			xpath.setNamespaceContext( new SimpleNamespaceContext( map));
			List<Node> paths = xpath.selectNodes(template);
			QName attr_qname=new QName("point",new Namespace("xtmpl",XTMPL_URI));
			for(Node n : paths) {
				if(!(n instanceof Element))
					continue;
				Element e=(Element)n;
				Attribute attr=e.attribute(attr_qname);
				String key=attr.getText();
				String path=e.getPath();
				e.remove(attr);
				attach.put(key,path);
			}
			removeNamespaces(template.getDocument().getRootElement());
			document=template;
		} catch (JaxenException e) {
			throw new InvalidXTmplException("Cannot parse template file",e);
		}
	}
	
	public XTmplDocument makeDocument() {
		return new XTmplDocument(document,attach);
	}
}
