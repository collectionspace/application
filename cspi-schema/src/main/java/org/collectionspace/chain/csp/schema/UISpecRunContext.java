package org.collectionspace.chain.csp.schema;

public class UISpecRunContext {
	private String affix=""; /* Eg in .csc-foo-bar-baz */
	private String ui_prefix=null; /* Eg in ${foo.0.bar.0.baz} */
	private UISpecRunContext parent=null;
	
	public UISpecRunContext() {}
	private UISpecRunContext(UISpecRunContext p) {
		parent = p;
	}
	
	public void appendAffix(String suffix) {
		affix += suffix;
	}
	public void setUIPrefix(String p) {
		ui_prefix = p;
	}
	public String getAffix() { 
		String prefix="";
		if(parent!=null)
			prefix = parent.getAffix();
		return prefix+affix;
	}
	public UISpecRunContext createChild() {
		return new UISpecRunContext(this);
	}
}
