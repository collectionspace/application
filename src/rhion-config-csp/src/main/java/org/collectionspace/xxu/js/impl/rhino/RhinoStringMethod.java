package org.collectionspace.xxu.js.impl.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class RhinoStringMethod extends RhinoBaseFunction implements Function {
	private String what;
	
	RhinoStringMethod(Scriptable scope,String what) {
		super(scope);
		this.what=what;
	}
	
	public Object call(Context arg0, Scriptable arg1, Scriptable arg2,Object[] arg3) {
		return what;
	}
}
