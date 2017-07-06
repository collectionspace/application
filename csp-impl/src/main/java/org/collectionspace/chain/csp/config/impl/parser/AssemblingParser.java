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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class AssemblingParser {
	private static final Logger logger = LoggerFactory.getLogger(AssemblingParser.class);
	
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
		String errMsg = String.format("Config Generation: '%s' - Exception raised during parsing.", 
				this.getMain().getPublicId());
		try {
			String rootpath=AssemblingParser.class.getPackage().getName().replaceAll("\\.","/")+"/"+root_file;
			InputStream root=Thread.currentThread().getContextClassLoader().getResourceAsStream(rootpath); // load the file at org/collectionspace/chain/csp/config/impl/parser/root.xml
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			TransformerHandler xform=transfactory.newTransformerHandler();
			xform.setResult(out);
			
			AssemblingContentHandler assemblingContentHandler = new AssemblingContentHandler(this,xform);
			logger.info(String.format("Temporary XMLMerge files will be written out to '%s'.", AssemblingContentHandler.getTempDirectory()));
			
			reader.setContentHandler(assemblingContentHandler);
			reader.parse(new InputSource(root));
		} catch(IOException e) {
			throw new ConfigException(errMsg, e);
		} catch (ParserConfigurationException e) {
			throw new ConfigException(errMsg, e);
		} catch (SAXException e) {
			throw new ConfigException(errMsg, e);
		} catch (TransformerConfigurationException e) {
			throw new ConfigException(errMsg, e);
		}
	}
}
