package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.List;

public class UISpecRunContext {
	private String ui_prefix=null; /* Eg in ${foo.0.bar.0.baz} */
	private String ui_affix=null; /* Eg in .csc-foo-bar-baz */
	private UISpecRunContext parent=null;
	
	public UISpecRunContext() {}
	private UISpecRunContext(UISpecRunContext p) {
		parent = p;
	}
	
	public void setUIAffix(String p) {
		ui_affix = p;
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
	private void getUIAffix(List<String> out) {
		if(parent!=null)
			parent.getUIAffix(out);
		if(ui_affix!=null)
			out.add(ui_affix);
	}
	
	public String[] getUIPrefix() {
		List <String> out = new ArrayList<String>();
		getUIPrefix(out);
		return out.toArray(new String[0]);
	}
	
	public String[] getUIAffix() { 
		List <String> out = new ArrayList<String>();
		getUIAffix(out);
		return out.toArray(new String[0]);
	}
	public UISpecRunContext createChild() {
		return new UISpecRunContext(this);
	}
}
