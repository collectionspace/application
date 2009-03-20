package org.collectionspace.xxu.impl;

import org.xml.sax.SAXException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.collectionspace.xxu.api.CSP;
import org.collectionspace.xxu.api.CSPMetadata;
import org.collectionspace.xxu.api.CSPProvider;
import org.collectionspace.xxu.api.ConfigLoadingException;


// XXX versioning
public class CSPImpl implements CSP {
	private static final String METADATA_PATH="/META-INF/csp.xml";
	
	private CSPMetadataImpl metadata;
	private ConfigLoaderImpl loader;
	private File file;
	private SAXParserFactory factory;
	
	public CSPImpl(ConfigLoaderImpl loader) {
		this.loader=loader;
		factory = SAXParserFactory.newInstance();
		factory.setXIncludeAware(true);
		factory.setNamespaceAware(true);
	}
	
	public CSPMetadata getMetadata() { return metadata; }
	public ConfigLoaderImpl getLoader() { return loader; }
	
	private void parseMetadata(InputStream in,File path) throws ConfigLoadingException {
		ConfigErrorHandler errors=new ConfigErrorHandler(loader.getMessages());
		try {
			SAXParser parser = factory.newSAXParser();
			SAXReader reader = new SAXReader( parser.getXMLReader());
			reader.setErrorHandler(errors);
			metadata=new CSPMetadataImpl(this,reader.read(in,"file://"+path.getAbsolutePath()));
		} catch (Throwable t) {
			errors.any_error(t);
		}
		errors.fail_if_necessary();
	}
	
	public InputStream getFileStream(String name) throws ConfigLoadingException {
		try {
			return new FileInputStream(new File(file.getAbsoluteFile()+"/"+name));
		} catch (FileNotFoundException e) {
			throw new ConfigLoadingException("Could not laod file from CSP: "+name,e);
		}
	}
	
	void loadFromDirectory(File in) throws ConfigLoadingException {
		file=in;
		if(!in.exists() || !in.isDirectory())
			throw new ConfigLoadingException(in.getAbsolutePath()+" does not exist, or is not a directory.");
		File csp_file=new File(in.getAbsolutePath()+METADATA_PATH);
		if(!csp_file.exists() || !csp_file.isFile())
			throw new ConfigLoadingException(csp_file.getAbsolutePath()+" does not exist of is not a file");
		try {
			parseMetadata(new FileInputStream(csp_file),csp_file);
		} catch (FileNotFoundException e) {
			throw new ConfigLoadingException(csp_file.getAbsolutePath()+" does not exist of is not a file");
		}
	}
	
	void loadFromZip(InputStream in) {
		// TODO Auto-generated method stub

	}
	
	void act(ConfigLoaderImpl in) throws ConfigLoadingException {
		metadata.act(this,in);
	}
}
