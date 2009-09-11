package org.collectionspace.chain.config.main.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.collectionspace.chain.config.main.ConfigRoot;
import org.collectionspace.chain.config.main.ConfigFactory;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.ConfigConsumer;
import org.collectionspace.csp.api.config.ConfigProvider;
import org.collectionspace.csp.api.config.ConfigContext;
import org.collectionspace.csp.api.config.EventContext;
import org.collectionspace.csp.api.config.EventConsumer;
import org.collectionspace.csp.api.core.CSPDependencyException;
import org.collectionspace.csp.impl.core.CSPManagerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class MainConfigFactoryImpl implements ConfigFactory, EventConsumer, ConfigContext {
	private SAXParserFactory factory;
	private SAXTransformerFactory transfactory;
	private ConfigLoadingMessages messages;
	private List<byte[]> xslts=new ArrayList<byte[]>();
	private EventConsumer consumer=this;
	private RootBarbWirer manager=new RootBarbWirer();
	private Set<ConfigProvider> providers=new HashSet<ConfigProvider>();
	
	/* Only set during testing */
	void setConsumer(EventConsumer in) { consumer=in; }
	
	public BarbWirer getRootBarbWirer() { return manager; }
	
	public void addConfigProvider(ConfigProvider provider) { providers.add(provider); }
	
	// XXX shouldn't throw CSPDependencyException
	public MainConfigFactoryImpl(CSPManagerImpl context) throws ConfigLoadFailedException, CSPDependencyException {
		factory = SAXParserFactory.newInstance();
		System.err.println(factory.getClass());
		factory.setNamespaceAware(true);
		TransformerFactory tf=TransformerFactory.newInstance();
		if (!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigLoadFailedException("XSLT transformer doesn't support SAX!");
		transfactory=(SAXTransformerFactory)tf;
		messages=new ConfigLoadingMessagesImpl(); // In the end we probably want to pass this in
		providers=new HashSet<ConfigProvider>(context.getConfigProviders());
		context.runConfigConsumers(this);
	}
	
	public void addXSLT(InputStream in) throws ConfigLoadFailedException {
		try {
			xslts.add(IOUtils.toByteArray(in));
		} catch (IOException e) {
			throw new ConfigLoadFailedException("Cannot add XSLT",e);
		}
	}
	
	public ConfigRoot parseConfig(InputSource src,String url) throws ConfigLoadFailedException {
		ConfigImpl out=new ConfigImpl();
		ConfigErrorHandler errors=new ConfigErrorHandler(messages);
		try {
			TransformerHandler[] xform=new TransformerHandler[xslts.size()];
			for(int i=0;i<xform.length;i++) {
				xform[i]=transfactory.newTransformerHandler(new StreamSource(new ByteArrayInputStream(xslts.get(i))));
			}
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			ContentHandler content_handler=new MainConfigHandler(consumer);
			ContentHandler first=content_handler;
			if(xform.length>0)
				first=xform[0];
			reader.setContentHandler(first);
			reader.setErrorHandler(errors);
			for(int i=1;i<xform.length;i++)
				xform[i-1].setResult(new SAXResult(xform[i]));
			if(xform.length>0)
				xform[xform.length-1].setResult(new SAXResult(content_handler));
			if(url!=null)
				src.setSystemId(url);
			// Probably some CSP stuff here
			reader.parse(src); // <-- The important line which kicks off the whole parse process
			for(ConfigProvider provider : providers)
				provider.provide(out); // <-- Providers will callback into addConfig
		} catch(Throwable t) {
			errors.any_error(t);
			errors.fail_if_necessary();
		}
		errors.fail_if_necessary();
		return out;
	}

	public void start(int ev,EventContext context) {
		manager.getConsumer().start(ev,context);
	}
	
	public void end(int ev,EventContext context) {
		manager.getConsumer().end(ev,context);
	}

	public void text(int ev,EventContext context,String text) {
		manager.getConsumer().text(ev,context,text);
	}

	public String getName() {
		return manager.getConsumer().getName();
	}
}
