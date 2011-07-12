package org.collectionspace.chain.csp.config.impl.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.collectionspace.chain.csp.config.ConfigException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

// XXX namespaces in XSLT
public class AssemblingContentHandler extends DefaultHandler implements ContentHandler {
	private static final String XSLT_TAG="xslt";
	private static final String INCLUDE_TAG="include";	
	private static final String DEFINE_TAG="define";
	
	private static final class XSLTTag { private String src,root; }
	private static final class IncludeTag { private String src; private boolean strip; }
	
	private AssemblingContentHandler resolve_parent;
	private SAXParserFactory factory;
	private ContentHandler delegated,up;
	private String delegated_root;
	private String tenantname;
	private int delegated_depth=-1,depth=0;
	private SAXTransformerFactory transfactory;
	private boolean outer,strip;
	private AssemblingParser parser;
	private Map<String,XSLTTag> xslt_tags=new HashMap<String,XSLTTag>();
	private Map<String,IncludeTag> include_tags=new HashMap<String,IncludeTag>();
	
	
	AssemblingContentHandler(AssemblingParser parser,ContentHandler r) throws ConfigException { this(parser,r,true,false,null); }
	
	AssemblingContentHandler(AssemblingParser parser,ContentHandler r,boolean outer,boolean strip,AssemblingContentHandler rp) throws ConfigException {
		up=r;
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		TransformerFactory tf=TransformerFactory.newInstance();
		if(!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigException("XSLT not supported");
		transfactory=(SAXTransformerFactory)tf;
		this.outer=outer;
		this.strip=strip;
		this.resolve_parent=rp;
		if(rp!=null){
			this.tenantname=rp.tenantname;
		}
		this.parser=parser;
	}

	// XXX test bad filename
	private void apply_xslt(InputSource xslt,String root) throws SAXException {
		try {
			ContentHandler inner=new AssemblingContentHandler(parser,up,false,false,this);
			TransformerHandler transformer=transfactory.newTransformerHandler(new StreamSource(xslt.getByteStream()));
			transformer.setResult(new SAXResult(inner));
			delegated=transformer;
			delegated.startDocument();
			if(root!=null)
				delegated.startElement("",root,root,new AttributesImpl());
			delegated_root=root;
			delegated_depth=1;
		} catch (TransformerConfigurationException e) {
			throw new SAXException("Could not create inner parser",e);
		} catch (ConfigException e) {
			throw new SAXException("Could not create inner parser",e);
		}
		
	}
	
	private void apply_include(InputSource src,boolean strip) throws SAXException {
		try {
			SAXParser sp=factory.newSAXParser();
			DefaultHandler inner=new AssemblingContentHandler(parser,up,false,strip,this);
			sp.parse(src,inner);
		} catch (ParserConfigurationException e) {
			throw new SAXException("Could not create inner parser",e);
		} catch (ConfigException e) {
			throw new SAXException("Could not create inner parser",e);
		} catch (IOException e) {
			throw new SAXException("Could not create inner parser",e);
		}
	}
	
	
	XSLTTag resolveXSLTTag(String name) {
		XSLTTag out=xslt_tags.get(name);
		if(out!=null)
			return out;
		if(resolve_parent!=null)
			return resolve_parent.resolveXSLTTag(name);
		return null;
	}
	IncludeTag resolveIncludeTag(String name) {
		IncludeTag out=include_tags.get(name);
		if(out!=null)
			return out;
		if(resolve_parent!=null)
			return resolve_parent.resolveIncludeTag(name);
		return null;		
	}
	
	private XSLTTag isXSLTTag(String localName,Attributes attributes) throws SAXException {
		XSLTTag out=resolveXSLTTag(localName);
		if(out!=null)
			return out;
		if(XSLT_TAG.equals(localName)) {
			out=new XSLTTag();
			out.src=attributes.getValue("src");
			out.root=attributes.getValue("root");
			return out;
		} else {
			return null;
		}
	}
	
	
	private IncludeTag isIncludeTag(String localName,Attributes attributes) throws SAXException {
		IncludeTag out=resolveIncludeTag(localName);
		if(out!=null)
			return out;
		if(INCLUDE_TAG.equals(localName)) {
			out=new IncludeTag();
			out.src=attributes.getValue("src");
			out.strip=stringToBoolean(attributes.getValue("strip-root"));
			return out;
		} else {
			return null;
		}		
	}

	private static boolean stringToBoolean(String in) {
		if(in!=null) {
			in=in.toLowerCase();
			return (in.equals("yes") || in.equals("true"));
		}
		return false;
	}

	private void processXSLTDefine(Attributes attr) throws SAXException {
		XSLTTag tag=new XSLTTag();
		tag.src=attr.getValue("src");
		tag.root=attr.getValue("root");
		String name=attr.getValue("tag");
		if(name==null || "".equals(name))
			throw new SAXException("Tag has no name in definition");
		xslt_tags.put(name,tag);
	}

	private void processIncludeDefine(Attributes attr) throws SAXException {
		IncludeTag tag=new IncludeTag();
		tag.src=attr.getValue("src");
		tag.strip=stringToBoolean(attr.getValue("strip-root"));
		String name=attr.getValue("tag");
		if(name==null || "".equals(name))
			throw new SAXException("Tag has no name in definition");
		include_tags.put(name,tag);
	}
	
	private boolean processDefines(String localName,Attributes attributes) throws SAXException {
		if(!DEFINE_TAG.equals(localName))
			return false;
		if(resolve_parent!=null && stringToBoolean(attributes.getValue("global"))) {
			return resolve_parent.processDefines(localName,attributes);
		}
		String mode=attributes.getValue("mode");
		if("xslt".equals(mode))
			processXSLTDefine(attributes);
		else if("include".equals(mode))
			processIncludeDefine(attributes);
		else
			throw new SAXException("Bad or missing include tag in define");
		return true;
	}
	
	public InputSource find_entity(String all) throws SAXException,IOException {
		SAXException e=null;
		for(String src : all.split(",")) {
			try {
				String test = this.tenantname;
				InputSource out=resolveEntity(null,"tenants/"+this.tenantname+"/"+src);
				if(out!=null)
					return out;
			} catch(SAXException x) { e=x; }
			try {
				InputSource out=resolveEntity(null,"defaults/"+src);
				if(out!=null)
					return out;
			} catch(SAXException x) { e=x; }
			try {
				InputSource out=resolveEntity(null,src);
				if(out!=null)
					return out;
			} catch(SAXException x) { e=x; }
		}
		throw new SAXException(all+" file(s) not found",e);
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		depth++;
		if(depth==1 && strip)
			return;
		if(delegated_depth>0) {
			delegated.startElement(uri, localName, qName, attributes);
			delegated_depth++;
			return;
		} else {
			try {
				if(processDefines(localName,attributes))
					return;
				XSLTTag xslt=isXSLTTag(localName,attributes);
				IncludeTag include=isIncludeTag(localName,attributes);
				if(localName.equals("cspace-config")){
					this.tenantname = attributes.getValue("tenantname");
				}
				if(xslt!=null) {
					apply_xslt(resolveEntity(null,xslt.src),xslt.root);
				}  else if(include!=null) {
					if("@".equals(include.src))
						apply_include(parser.getMain(),include.strip);
					else
						apply_include(find_entity(include.src),include.strip);
				} else {
					up.startElement(uri, localName, qName, attributes);
				}
			} catch(IOException x) {
				throw new SAXException("Could not load source");
			}
		}
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		depth--;
		if(depth==0 && strip)
			return;
		if(delegated_depth>0) {
			delegated_depth--;
			if(delegated_depth>0) {
				delegated.endElement(uri, localName, qName);
			} else {
				if(delegated_root!=null)
					delegated.endElement("",delegated_root,delegated_root);
				delegated.endDocument();
			}
			return;
		}
		// XXX should check skipped tags instead, to make it more robust to internal definitions
		XSLTTag xslt=isXSLTTag(localName,new AttributesImpl());
		IncludeTag include=isIncludeTag(localName,new AttributesImpl());
		if(include!=null || xslt!=null || DEFINE_TAG.equals(localName))
			return;
		up.endElement(uri, localName, qName);
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(delegated_depth>0)
			delegated.characters(ch,start,length);
		else
			up.characters(ch,start,length);
	}
	
	public void startDocument() throws SAXException {
		if(outer)
			up.startDocument();
	}

	public void endDocument() throws SAXException {
		if(outer)
			up.endDocument();
	}
	
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		return parser.getEntityResolver().resolveEntity(publicId,systemId);
	}
}
