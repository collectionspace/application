package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.impl.XMLEventConsumer;
import org.collectionspace.chain.config.main.impl.XMLEventContext;

public class StringXMLEventConsumer implements XMLEventConsumer {
	private StringBuffer out=new StringBuffer();
	
	public void end(int ev, XMLEventContext context) {
		out.append(ev+" end "+context.dumpStack()+"\n");
	}

	public void start(int ev, XMLEventContext context) {
		out.append(ev+" start "+context.dumpStack()+"\n");
	}

	public void text(int ev, XMLEventContext context, String text) {
		out.append(ev+" text {"+text+"} "+context.dumpStack()+"\n");
	}

	public String toString() { return out.toString(); }
}
