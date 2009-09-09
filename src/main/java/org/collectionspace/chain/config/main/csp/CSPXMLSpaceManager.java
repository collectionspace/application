package org.collectionspace.chain.config.main.csp;

import org.collectionspace.chain.config.main.XMLEventConsumer;

public interface CSPXMLSpaceManager {
	public CSPXMLSpaceAttachmentPoint getAttachmentPoint(String name);
	public XMLEventConsumer getConsumer();
}
