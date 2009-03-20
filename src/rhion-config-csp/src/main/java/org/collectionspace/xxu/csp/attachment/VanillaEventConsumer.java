package org.collectionspace.xxu.csp.attachment;

import org.collectionspace.xxu.api.ConfigLoader;
import org.collectionspace.xxu.api.CSPConfig;
import org.collectionspace.xxu.api.XMLEventConsumer;
import org.collectionspace.xxu.api.XMLEventContext;
import org.collectionspace.xxu.impl.ConfigImpl;

public class VanillaEventConsumer implements XMLEventConsumer {
	private ConfigLoader loader;
	
	public VanillaEventConsumer(ConfigLoader cfg) {
		loader=cfg;
	}
		
	public void end(int ev, XMLEventContext context) {}

	public void start(int ev, XMLEventContext context) {}

	// XXX also empty tags
	public void text(int ev, XMLEventContext context, String text) {
		CSPConfig cfg=loader.getCSPConfig();
		String[] stack=context.getStack();
		for(int i=0;i<stack.length-1;i++) {
			CSPConfig next=cfg.step(stack[i]);
			if(next==null) {
				next=new ConfigImpl();
				cfg.attach(stack[i],next);
			}
			cfg=next;
		}
		cfg.set(stack[stack.length-1],text);
		System.err.println("Saved unclaimed text for "+ev+" in "+this);
	}

}
