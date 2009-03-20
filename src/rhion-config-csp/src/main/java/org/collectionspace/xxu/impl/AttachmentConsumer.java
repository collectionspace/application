package org.collectionspace.xxu.impl;

import org.collectionspace.xxu.api.XMLEventConsumer;

public class AttachmentConsumer {
	private XMLEventConsumer events;
	private String name;
	
	AttachmentConsumer(XMLEventConsumer e,String n) { events=e; name=n; }
	
	public XMLEventConsumer getEventConsumer() { return events; }
	public String getName() { return name; }
}
