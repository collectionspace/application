package org.collectionspace.chain.config.main.csp;

public interface CSPXMLSpaceAttachmentPoint {
	public CSPXMLSpaceManager getManager();
	public void attach(CSPXMLSpaceManager manager,String root);
}
