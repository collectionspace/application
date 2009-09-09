package org.collectionspace.chain.config.main.impl;

import org.collectionspace.csp.api.config.EventContext;

public class EventContextImpl implements EventContext {
	private String[] stack;
	private EventContext parent;

	public EventContextImpl(EventContext parent,String[] in) {
		this.parent=parent;
		this.stack=in;
	}

	public String[] getStack() { return stack; }

	public String getPath() {	
		StringBuffer out=new StringBuffer();
		boolean first=true;
		for(String row : stack) {
			if(!first)
				out.append('/');
			out.append(row);
			first=false;
		}
		return out.toString(); 
	}

	public String dumpStack() { 
		StringBuffer out=new StringBuffer();
		out.append(getPath());
		if(parent!=null) {
			out.append(":::");
			out.append(parent.dumpStack());
		}
		return out.toString();
	}

	public EventContext getParentContext() { return parent; }
}
