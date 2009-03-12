package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class RhinoWrapFactory extends WrapFactory {	
	@SuppressWarnings("unchecked")
	public Object wrap(Context cx,Scriptable scope,Object obj,Class staticType) {
		try {
			if(obj!=null) {
				if(obj.getClass().isArray() || obj instanceof List)
					return new RhinoSequenceWrapper(cx,scope,obj);
				if(obj instanceof Map)
					return new RhinoMapWrapper(cx,scope,(Map)obj);
			}
			return new RhinoWrapper(cx,scope,obj);
		} catch (JavascriptException e) {
			RhinoContext.static_log(cx,e.getMessage());
			return null;
		}
	}
}
