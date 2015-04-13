package org.collectionspace.chain.csp.config.impl.parser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.collectionspace.chain.csp.config.ConfigException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class AssemblingParser {
	private EntityResolver er;
	private String root_file="root.xml";
	private SAXParserFactory factory;
	private SAXTransformerFactory transfactory;
	private InputSource main;
	
	public AssemblingParser(EntityResolver er,InputSource in) throws ConfigException {
		this.er=er;
		this.main=in;
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		
		TransformerFactory tf=TransformerFactory.newInstance();
		if(!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigException("XSLT not supported");
		transfactory=(SAXTransformerFactory)tf;
	}
	
	void setRootFile(String in) { /* For testing only */ root_file=in; }
	EntityResolver getEntityResolver() { return er; }
	InputSource getMain() { return main; }
	
	public void parse(Result out) throws ConfigException {
		try {
			String rootpath=AssemblingParser.class.getPackage().getName().replaceAll("\\.","/")+"/"+root_file;
			InputStream root=Thread.currentThread().getContextClassLoader().getResourceAsStream(rootpath); // load the file at org/collectionspace/chain/csp/config/impl/parser/root.xml
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			TransformerHandler xform=transfactory.newTransformerHandler();
			xform.setResult(out);
			reader.setContentHandler(new AssemblingContentHandler(this,xform));
			reader.parse(new InputSource(root));
		} catch(IOException e) {
			throw new ConfigException("Exception raised during parsing",e);
		} catch (ParserConfigurationException e) {
			throw new ConfigException("Exception raised during parsing",e);
		} catch (SAXException e) {
			throw new ConfigException("Exception raised during parsing",e);
		} catch (TransformerConfigurationException e) {
			throw new ConfigException("Exception raised during parsing",e);
		}
	}
}
