package org.collectionspace.xxu.js.impl.rhino;

import org.collectionspace.xxu.js.api.JavascriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class RhinoWrapFactory extends WrapFactory {	
	@SuppressWarnings("unchecked")
	public Object wrap(Context cx,Scriptable scope,Object obj,Class staticType) {
		try {
			return new RhinoWrapper(cx,scope,obj);
		} catch (JavascriptException e) {
			System.err.println(e.getMessage()); // XXX log it
			return null;
		}
	}
}
