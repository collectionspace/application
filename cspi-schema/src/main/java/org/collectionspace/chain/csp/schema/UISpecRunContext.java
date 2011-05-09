package org.collectionspace.chain.csp.schema;

public class UISpecRunContext {
	private String affix="";
	
	public String getAffix() { return affix; }
	
	public void appendAffix(String rest) { affix += rest; }
}
