package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;

/* Simple CSPXMLSpaceManager for the root: single attachment point called root. */
public class RootCSPXMLSpaceManager extends SimpleCSPXMLSpaceManager implements CSPXMLSpaceManager {
	public RootCSPXMLSpaceManager() {
		addAttachmentPoint("root",new String[]{});
	}

	public RootCSPXMLSpaceManager(String name) {
		super(name);
		addAttachmentPoint("root",new String[]{});
	}
}
