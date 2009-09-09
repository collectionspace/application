package org.collectionspace.chain.config.main.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.config.api.ConfigLoadFailedException;
import org.collectionspace.chain.config.main.ConfigErrorHandler;
import org.collectionspace.chain.config.main.MainConfig;
import org.collectionspace.chain.config.main.MainConfigFactory;
import org.collectionspace.chain.config.main.XMLEventConsumer;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class MainConfigFactoryImpl implements MainConfigFactory, XMLEventConsumer {
	private SAXParserFactory factory;
	private SAXTransformerFactory transfactory;
	private ConfigLoadingMessages messages;
	private List<byte[]> xslts=new ArrayList<byte[]>();
	private XMLEventConsumer consumer=this;
	private RootCSPXMLSpaceManager manager=new RootCSPXMLSpaceManager("root");

	/* Only set during testing */
	void setConsumer(XMLEventConsumer in) { consumer=in; }
	
	public CSPXMLSpaceManager getCSPXMLSpaceManager() { return manager; }
	
	public MainConfigFactoryImpl(ConfigLoadingMessages messages) throws ConfigLoadFailedException {
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(true);
		TransformerFactory tf=TransformerFactory.newInstance();
		if (!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigLoadFailedException("XSLT transformer doesn't support SAX!");
		transfactory=(SAXTransformerFactory)tf;
		this.messages=messages;
		// CSP stuff will go here
	}
	
	public void addXSLT(InputStream in) throws ConfigLoadFailedException {
		try {
			xslts.add(IOUtils.toByteArray(in));
		} catch (IOException e) {
			throw new ConfigLoadFailedException("Cannot add XSLT",e);
		}
	}
	
	public MainConfig parseConfig(InputSource src,String url) throws ConfigLoadFailedException {
		ConfigErrorHandler errors=new ConfigErrorHandler(messages);
		try {
			TransformerHandler[] xform=new TransformerHandler[xslts.size()];
			for(int i=0;i<xform.length;i++) {
				xform[i]=transfactory.newTransformerHandler(new StreamSource(new ByteArrayInputStream(xslts.get(i))));
			}
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			ContentHandler out=new MainConfigHandler(consumer);
			ContentHandler first=out;
			if(xform.length>0)
				first=xform[0];
			reader.setContentHandler(first);
			reader.setErrorHandler(errors);
			for(int i=1;i<xform.length;i++)
				xform[i-1].setResult(new SAXResult(xform[i]));
			if(xform.length>0)
				xform[xform.length-1].setResult(new SAXResult(out));
			if(url!=null)
				src.setSystemId(url);
			// Some CSP stuff
			reader.parse(src);
		} catch(Throwable t) {
			errors.any_error(t);
			errors.fail_if_necessary();
		}
		errors.fail_if_necessary();
		return null; // XXX
	}

	public void start(int ev,XMLEventContext context) {
		manager.getConsumer().start(ev,context);
	}
	
	public void end(int ev,XMLEventContext context) {
		manager.getConsumer().end(ev,context);
	}

	public void text(int ev,XMLEventContext context,String text) {
		manager.getConsumer().text(ev,context,text);
	}

	public String getName() {
		return manager.getConsumer().getName();
	}
}
