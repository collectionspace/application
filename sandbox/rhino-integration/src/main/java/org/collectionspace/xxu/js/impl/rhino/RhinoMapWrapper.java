package org.collectionspace.xxu.js.impl.rhino;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RhinoMapWrapper implements Scriptable {
	private Map map;
	private Scriptable scope;
	private Context ctx;
	
	RhinoMapWrapper(Context ctx,Scriptable scope,Map thing) {
		this.ctx=ctx;
		this.scope=scope;
		this.map=thing;
	}
	
	public Map getThing() { return map; }
	
	public void delete(String key) { map.remove(key); }
	public void delete(int arg0) {} // No!
	public Object get(String key, Scriptable start) { return RhinoContext.staticWrapIfNeeded(ctx,scope,map.get(key)); }
	public Object get(int arg0, Scriptable arg1) { return Scriptable.NOT_FOUND; } // No!
	public String getClassName() { return "Object"; }

	public Object getDefaultValue(Class hint) {
		if(hint==Boolean.class)
			return true;
		if(hint==Number.class)
			return null;
		return map.toString();
	}

	public Object[] getIds() { return map.keySet().toArray(new Object[0]); }
	public Scriptable getParentScope() { return scope; }

	public Scriptable getPrototype() {
		Scriptable out=ScriptableObject.getClassPrototype(scope,"Object");
		return out;
	}

	public boolean has(String key, Scriptable start) { return map.containsKey(key); }
	public boolean has(int arg0, Scriptable arg1) { return false; }
	public boolean hasInstance(Scriptable arg0) { return false; }
	public void put(String key, Scriptable start, Object value) { map.put(key,value); }
	public void put(int arg0, Scriptable arg1, Object arg2) {} // No!
	public void setParentScope(Scriptable s) { scope=s; }
	public void setPrototype(Scriptable arg0) {} // No!
}
