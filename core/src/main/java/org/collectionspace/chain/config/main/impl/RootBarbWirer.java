package org.collectionspace.chain.config.main.impl;

import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.helper.config.SimpleBarbWirer;

/* Simple CSPXMLSpaceManager for the root: single attachment point called root. */
public class RootBarbWirer extends SimpleBarbWirer implements BarbWirer {
	public RootBarbWirer() {
		super("root");
		addAttachmentPoint("root",new String[]{});
	}
}
