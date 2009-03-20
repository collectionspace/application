package org.collectionspace.xxu.api;

import java.io.File;
import java.io.InputStream;

import org.collectionspace.xxu.impl.ConfigImpl;

public interface ConfigLoader {
	public CSP loadCSPFromZip(InputStream in) throws ConfigLoadingException;
	public CSP loadCSPFromDirectory(File in) throws ConfigLoadingException;
	public void addCSP(CSP in) throws ConfigLoadingException;
	public void loadConfigFromXML(InputStream in,File path) throws ConfigLoadingException;
	public Config getConfig();
	public void setMessages(ConfigLoadingMessages m);
	
	/* You probably don't want these unless you're a CSP, or something */
	public void addXSLT(InputStream in) throws ConfigLoadingException;
	public void registerAttachmentPoint(String parent,String[] rest,String name) throws ConfigLoadingException;
	void registerAttachment(String point,String tag,XMLEventConsumer consumer);
	public CSPConfig pushCSPConfig();
	public CSPConfig getCSPConfig();
	public CSPConfig popCSPConfig();	
}
