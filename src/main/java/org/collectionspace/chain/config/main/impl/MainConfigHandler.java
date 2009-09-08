package org.collectionspace.chain.config.main.impl;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class MainConfigHandler extends DefaultHandler {
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		System.err.println("<"+localName+">");
	}
	
	public void endElement(String uri, String localName, String qName) {
		System.err.println("</"+localName+">");
	}
	
	public void characters(char[] ch, int start, int length) {
		System.err.println("{"+new String(ch,start,length)+"}");
	}
}
