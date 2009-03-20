package org.collectionspace.xxu.js.impl.rhino;

import org.collectionspace.xxu.js.api.JavascriptScript;
import org.mozilla.javascript.Script;

public class RhinoScript implements JavascriptScript {
	private RhinoSystem system;
	private Script the_script;
	private String contents,name="<no name>";
	private int line=1;
	
	RhinoScript(RhinoSystem system) { this.system=system; }
	
	public void setScript(String in) { contents=in; }
	
	public RhinoSystem getSystem() { return system; }
	
	// XXX support security domains
	Script getScript() { 
		if(the_script!=null)
			return the_script;
		the_script=system.getContext().compileString(contents,name,line,null);
		return the_script;
	}
}
