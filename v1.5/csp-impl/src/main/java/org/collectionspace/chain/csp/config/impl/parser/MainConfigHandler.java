/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.config.impl.parser;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class MainConfigHandler extends DefaultHandler {
	private static final String XMLNS="http://www.w3.org/2000/xmlns/";
	private static final String XML="http://www.w3.org/XML/1998/namespace";
	
	private EventConsumer events;
	private List<String> stack=new ArrayList<String>();
	private int ev=0;
	
	public MainConfigHandler(EventConsumer cfg) {
		this.events=cfg;
	}
		
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		String name=localName;
		if(uri!=null && !"".equals(uri))
			name=uri+":"+name;
		events.start(name);
		// Attributes:
		for(int i=0;i<attributes.getLength();i++) {
			String k=attributes.getLocalName(i);
			String auri=attributes.getURI(i);
			// skip xmlns:... attributes
			if(auri!=null && (XMLNS.equals(auri) || XML.equals(auri)))
				continue;
			if(auri!=null && !"".equals(auri))
				k=auri+":"+k;
			events.start("@"+k);
			events.text(attributes.getValue(i));
			events.end();
		}
	}
	
	public void endElement(String uri, String localName, String qName) {
		events.end();
	}
	
	private boolean entirelySpace(String text) {
		for(char c : text.toCharArray())
			if(!Character.isWhitespace(c))
				return false;
		return true;
	}
	
	public void characters(char[] ch, int start, int length) {
		String text=new String(ch,start,length);
		if(!entirelySpace(text))
			events.text(text);
	}
}
