package org.collectionspace.chain.config.main.impl;

public class XMLEventContextImpl implements XMLEventContext {
	private String[] stack;
	
	public XMLEventContextImpl(String[] in) { this.stack=in; }
	
	public String[] getStack() { return stack; }
	
	public String dumpStack() {
		StringBuffer out=new StringBuffer();
		for(String row : stack) {
			out.append(row);
			out.append('/');
		}
		return out.toString();
	}
}
