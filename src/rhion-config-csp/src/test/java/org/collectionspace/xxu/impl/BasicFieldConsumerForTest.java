package org.collectionspace.xxu.impl;

import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;

public class BasicFieldConsumerForTest extends XMLEventTester implements XMLEventConsumer {
	private ConfigLoaderImpl loader;
	private DelegatingAttachment delegate;
	
	public BasicFieldConsumerForTest(ConfigLoaderImpl loader,String[] in) { 
		super(in);
		this.loader=loader;
		delegate=new DelegatingAttachment(loader);
	}

	public void end(int ev, XMLEventContext context) {
		if(delegate.delegate_end(ev,context))
			return;
		super.end(ev,context);
	}

	public void start(int ev, XMLEventContext context) {
		if(delegate.delegate_start(ev,context))
			return;		
		super.start(ev,context);
	}

	public void text(int ev, XMLEventContext context, String text) {
		if(delegate.delegate_text(ev,context,text))
			return;
		super.text(ev,context,text);
	}
}
