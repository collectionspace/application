package org.collectionspace.csp.helper.config;

import org.collectionspace.csp.api.config.Barb;
import org.collectionspace.csp.api.config.BarbWirer;
import org.collectionspace.csp.api.config.EventConsumer;

/* Simple CSPXMLSpaceManager for the root: single attachment point called root. */
public class LeafBarbWirer implements BarbWirer {
	private EventConsumer consumer;
	
	public LeafBarbWirer(EventConsumer consumer) { this.consumer=consumer; }
	protected LeafBarbWirer() {}
	protected void setConsumer(EventConsumer consumer) { this.consumer=consumer; }
	
	public Barb getBarb(String name) { return null; }

	public EventConsumer getConsumer() { return consumer; }
}
