package org.collectionspace.chain.config.main.impl;

import org.collectionspace.csp.api.config.EventContext;
import org.collectionspace.csp.api.config.EventConsumer;

public class StringXMLEventConsumer implements EventConsumer {
	private StringBuffer out=new StringBuffer();
	
	public void end(int ev, EventContext context) {
		out.append(ev+" end "+context.dumpStack()+"\n");
	}

	public void start(int ev, EventContext context) {
		out.append(ev+" start "+context.dumpStack()+"\n");
	}

	public void text(int ev, EventContext context, String text) {
		out.append(ev+" text {"+text+"} "+context.dumpStack()+"\n");
	}

	public String toString() { return out.toString(); }

	public String getName() { return "string-consumer"; }
}
