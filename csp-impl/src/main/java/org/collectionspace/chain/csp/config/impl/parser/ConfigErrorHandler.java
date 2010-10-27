/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.parser;

import java.util.List;
import java.util.ArrayList;

import org.collectionspace.chain.csp.config.ConfigException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ConfigErrorHandler implements ErrorHandler {
	private List<Throwable> errors=new ArrayList<Throwable>();
	private List<Throwable> warnings=new ArrayList<Throwable>();
	private ConfigLoadingMessages messages;
	
	public ConfigErrorHandler(ConfigLoadingMessages m) {
		messages=m;
	}
	
	private void sendAsMessage(Throwable t,boolean error) {
		if(messages==null)
			return;
		StringBuffer out=new StringBuffer();
		while(t!=null) {
			out.append("Message: "+t.getMessage()+"\n");
			if(t instanceof SAXParseException) {
				out.append("File: "+((SAXParseException)t).getSystemId()+" ");
				out.append(" Line: "+((SAXParseException)t).getLineNumber()+" ");
			}
			out.append("\nStack : ");
			for(StackTraceElement elem : t.getStackTrace()) {
				out.append(elem.getClassName()+"."+elem.getMethodName()+" ("+
						   elem.getLineNumber()+")\n");
			}
			t=t.getCause();
			if(t!=null)
				out.append("\nCAUSED BY\n");
		}
		if(error)
			messages.error(out.toString());
		else
			messages.warn(out.toString());
	}
	
	public void any_error(Throwable e) {
		sendAsMessage(e,true);
		errors.add(e);		
	}

	public void any_warning(Throwable e) {
		sendAsMessage(e,true);
		warnings.add(e);
	}
	
	public void error(SAXParseException e) throws SAXException { any_error(e); }
	public void fatalError(SAXParseException e) throws SAXException { any_error(e); }
	public void warning(SAXParseException e) throws SAXException { any_warning(e); }

	public void fail_if_necessary() throws ConfigException { 
		if(errors.size()==0)
			return;
		StringBuffer out=new StringBuffer();
		out.append("Error loading config. See messages for details: summary ");
		for(Throwable t : errors) {
			out.append(t.getMessage());
			if(t instanceof SAXParseException) {
				SAXParseException s=(SAXParseException)t;
				out.append("["+s.getSystemId()+"/"+s.getLineNumber()+"]");
			}
			out.append(' ');
		}
		throw new ConfigException(out.toString());
	}
}
