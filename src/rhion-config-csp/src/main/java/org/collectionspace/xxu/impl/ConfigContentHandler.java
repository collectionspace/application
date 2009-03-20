package org.collectionspace.xxu.impl;

import java.util.ArrayList;
import java.util.List;

import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigContentHandler extends DefaultHandler {
	private static final String XMLNS="http://www.w3.org/2000/xmlns/";
	private static final String XML="http://www.w3.org/XML/1998/namespace";
	
	private XMLEventConsumer events;
	private List<String> stack=new ArrayList<String>();
	private int ev=0;
	
	public ConfigContentHandler(XMLEventConsumer cfg) {
		this.events=cfg;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		String name=localName;
		if(uri!=null && !"".equals(uri))
			name=uri+":"+name;
		stack.add(name);
		XMLEventContext ctx=new XMLEventRouter(stack.toArray(new String[0]));
		
		events.start(ev++,new XMLEventRouter(stack.toArray(new String[0])));
		// Attributes:
		for(int i=0;i<attributes.getLength();i++) {
			String k=attributes.getLocalName(i);
			String auri=attributes.getURI(i);
			// skip xmlns:... attributes
			if(auri!=null && (XMLNS.equals(auri) || XML.equals(auri)))
				continue;
			if(auri!=null && !"".equals(auri))
				k=auri+":"+k;
			stack.add("@"+k);
			events.start(ev++,new XMLEventRouter(stack.toArray(new String[0])));
			events.text(ev++,new XMLEventRouter(stack.toArray(new String[0])),attributes.getValue(i));
			events.end(ev++,new XMLEventRouter(stack.toArray(new String[0])));
			stack.remove(stack.size()-1);
		}
	}
	
	public void endElement(String uri, String localName, String qName) {
		events.end(ev++,new XMLEventRouter(stack.toArray(new String[0])));
		stack.remove(stack.size()-1);
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
			events.text(ev++,new XMLEventRouter(stack.toArray(new String[0])),text);
	}
}
