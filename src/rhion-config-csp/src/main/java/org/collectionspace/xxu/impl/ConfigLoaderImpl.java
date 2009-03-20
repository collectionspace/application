package org.collectionspace.xxu.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.Config;
import org.collectionspace.xxu.api.CSPConfig;
import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.ConfigLoadingMessages;
import org.collectionspace.xxu.api.ConfigLoadingException;
import org.collectionspace.xxu.api.CSPProviderFactory;
import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;
import org.collectionspace.xxu.csp.transform.ProviderTransformFactory;
import org.collectionspace.xxu.csp.attachment.ProviderAttachmentFactory;
import org.dom4j.io.SAXReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

// XXX new pfacts in csps
public class ConfigLoaderImpl implements ConfigLoader {
	private SAXParserFactory factory;
	private SAXTransformerFactory transfactory;
	private List<byte[]> xslts=new ArrayList<byte[]>();
	private List<CSPProviderFactory> pfact=new ArrayList<CSPProviderFactory>();
	private AttachmentRegistry attachments=new AttachmentRegistry();
	private XMLEventConsumer consumer=new DelegatingAttachment(this); // Only ever changed for testing
	private ConfigLoadingMessages messages;
	private List<ConfigImpl> configs=new ArrayList<ConfigImpl>();
	
	public ConfigLoaderImpl() throws ConfigLoadingException {
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(true);
		TransformerFactory tf=TransformerFactory.newInstance();
		if (!tf.getFeature(SAXSource.FEATURE) || !tf.getFeature(SAXResult.FEATURE))
			throw new ConfigLoadingException("XSLT transformer doesn't support SAX!");
		transfactory=(SAXTransformerFactory)tf;
		// Add the default providers
		pfact.add(new ProviderTransformFactory());
		pfact.add(new ProviderAttachmentFactory());
		// Add the default attachment point
		registerAttachmentPoint(null,new String[]{"config"},"root");
	}

	public CSPConfig pushCSPConfig() {
		ConfigImpl out=new ConfigImpl();
		configs.add(out);
		return out;
	}
	
	public CSPConfig getCSPConfig() {
		if(configs.size()==0)
			return null;
		return configs.get(configs.size()-1);
	}
	
	public CSPConfig popCSPConfig() {
		if(configs.size()==0)
			return null;
		return configs.remove(configs.size()-1);
	}
	
	public void setMessages(ConfigLoadingMessages m) { messages=m; }
	
	void setEventConsumerForTesting(XMLEventConsumer c) { consumer=c; } // Only for unit testing
	
	AttachmentConsumer[] resolveAndTruncate(XMLEventContext in) {
		return attachments.resolveAndTruncate(in);
	}
	
	public void registerAttachment(String point,String tag,XMLEventConsumer consumer) {
		attachments.registerConsumer(point,tag,consumer);
	}

	public void registerAttachmentPoint(String parent,String[] rest,String name) throws ConfigLoadingException {
		attachments.registerAttachmentPoint(parent,rest,name);
	}
	
	public void addCSP(CSP in) throws ConfigLoadingException {
		if(in instanceof CSPImpl)
			((CSPImpl)in).act(this);
	}

	public Config getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	// XXX proper csp detection in exceptions
	public void addXSLT(InputStream in) throws ConfigLoadingException {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			IOUtils.copy(in,baos);
		} catch (IOException e) {
			throw new ConfigLoadingException("Cannot save XSLT",e);
		}
		xslts.add(baos.toByteArray());
	}
	
	public ConfigLoadingMessages getMessages() { return messages; }
	
	public void loadConfigFromXML(InputStream in,File file) throws ConfigLoadingException {
		ConfigErrorHandler errors=new ConfigErrorHandler(messages);
		try {
			TransformerHandler[] xform=new TransformerHandler[xslts.size()];
			for(int i=0;i<xform.length;i++) {
				xform[i]=transfactory.newTransformerHandler(new StreamSource(new ByteArrayInputStream(xslts.get(i))));
			}
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			ContentHandler out=new ConfigContentHandler(consumer); // consumer=this, except during testing
			ContentHandler first=out;
			if(xform.length>0)
				first=xform[0];
			reader.setContentHandler(first);
			reader.setErrorHandler(errors);
			for(int i=1;i<xform.length;i++)
				xform[i-1].setResult(new SAXResult(xform[i]));
			if(xform.length>0)
				xform[xform.length-1].setResult(new SAXResult(out));
			InputSource src=new InputSource(in);
			if(file!=null)
				src.setSystemId("file://"+file.getAbsolutePath());
			pushCSPConfig();
			reader.parse(src);
		} catch(Throwable t) {
			errors.any_error(t);
			errors.fail_if_necessary();
		}
		errors.fail_if_necessary();
	}
	// XXX error handling
	
	public CSP loadCSPFromDirectory(File in) throws ConfigLoadingException {
		CSPImpl out=new CSPImpl(this);
		out.loadFromDirectory(in);
		return out;
	}

	public CSP loadCSPFromZip(InputStream in) throws ConfigLoadingException {
		CSPImpl out=new CSPImpl(this);
		out.loadFromZip(in);
		return out;
	}
	
	CSPProviderFactory[] getProviderFactories() { return pfact.toArray(new CSPProviderFactory[0]); }
}
