package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.XMLEventConsumer;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceAttachmentPoint;
import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;

/* Simple CSPXMLSpaceManager for the root: single attachment point called root. */
public class LeafCSPXMLSpaceManager implements CSPXMLSpaceManager {
	private XMLEventConsumer consumer;
	
	public LeafCSPXMLSpaceManager(XMLEventConsumer consumer) { this.consumer=consumer; }
	
	public CSPXMLSpaceAttachmentPoint getAttachmentPoint(String name) { return null; }

	public XMLEventConsumer getConsumer() { return consumer; }
}
