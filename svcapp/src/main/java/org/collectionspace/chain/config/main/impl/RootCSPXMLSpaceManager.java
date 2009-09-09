package org.collectionspace.chain.config.main.impl;

import org.collectionspace.chain.config.main.csp.CSPXMLSpaceManager;

/* Simple CSPXMLSpaceManager for the root: single attachment point called root. */
public class RootCSPXMLSpaceManager extends SimpleCSPXMLSpaceManager implements CSPXMLSpaceManager {
	public RootCSPXMLSpaceManager() {
		super("root");
		addAttachmentPoint("root",new String[]{});
	}
}
