package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.List;

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
	
	private void getUIPrefix(List<String> out) {
		if(parent!=null)
			parent.getUIPrefix(out);
		if(ui_prefix!=null)
			out.add(ui_prefix);
	}
	
	public String[] getUIPrefix() {
		List <String> out = new ArrayList<String>();
		getUIPrefix(out);
		return out.toArray(new String[0]);
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
