package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.Method;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public abstract class RhinoBaseFunction implements Function {
	protected Scriptable scope;
	
	RhinoBaseFunction(Scriptable scope) { this.scope=scope; }
	
	public Object[] getIds() { return new Object[0]; }
	public Scriptable getParentScope() { return scope; }
	@SuppressWarnings("unchecked") public Object getDefaultValue(Class arg0) { return "[XXU sys method]"; } // None
	public boolean has(String arg0, Scriptable arg1) { return false; } // None
	public boolean has(int arg0, Scriptable arg1) { return false; } // None
	public boolean hasInstance(Scriptable arg0) { return false; }
	public void put(String arg0, Scriptable arg1, Object arg2) {} // No
	public void put(int arg0, Scriptable arg1, Object arg2) {} // No
	public void setParentScope(Scriptable arg0) {} // No
	public void setPrototype(Scriptable arg0) {} // No
	public Scriptable construct(Context arg0, Scriptable arg1, Object[] arg2) { return null; } // No
	public void delete(String arg0) {} // No
	public void delete(int arg0) {} // No
	public Object get(int arg0, Scriptable arg1) { return Scriptable.NOT_FOUND; }
	
	public abstract Object call(Context arg0, Scriptable arg1, Scriptable arg2,Object[] arg3);

	public Scriptable getPrototype() { 
		Scriptable out=ScriptableObject.getClassPrototype(scope,"Function");
		return out;
	}
		
	public Object get(String method, Scriptable start) { 
		if("toString".equals(method))
			return new RhinoStringMethod(scope,"[XXU sys method]");
		return Scriptable.NOT_FOUND;
	}

	public String getClassName() { return "Function"; }
}
