package org.collectionspace.chain.config.main.impl;

public class XMLEventContextImpl implements XMLEventContext {
	private String[] stack;
	private XMLEventContext parent;

	public XMLEventContextImpl(XMLEventContext parent,String[] in) {
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

	public XMLEventContext getParentContext() { return parent; }
}
