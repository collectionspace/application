package org.collectionspace.xxu.js.impl.rhino;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class RhinoRegistry implements Scriptable {
	private Map<String,Object> props=new HashMap<String,Object>();
	private RhinoContext context;
	
	public RhinoRegistry(RhinoContext context) { this.context=context; }

	void register(String name,Object object) {
		props.put(name,context.wrapIfNeeded(object));
	}
	
	public void delete(String arg0) {} // no
	public void delete(int arg0) {} // no

	public Object get(String name, Scriptable obj) {
		Object out=props.get(name);
		if(out==null)
			return Scriptable.NOT_FOUND;
		return out;
	}

	public Object get(int idx, Scriptable obj) { return Scriptable.NOT_FOUND; }
	public String getClassName() { return RhinoRegistry.class.getCanonicalName(); }
	@SuppressWarnings("unchecked") public Object getDefaultValue(Class arg0) { return "sys"; }
	public Object[] getIds() { return props.keySet().toArray(new Object[0]); }
	public Scriptable getParentScope() { return context.getScope(); }
	public Scriptable getPrototype() { return null; }

	public boolean has(String key, Scriptable base) { return props.containsKey(key); }
	public boolean has(int arg0, Scriptable arg1) { return false; }

	public boolean hasInstance(Scriptable value) {
		Scriptable proto = value.getPrototype();
		while (proto != null) {
			if (proto.equals(this)) 
				return true;
			proto = proto.getPrototype();
		}
		return false;
	}

	public void put(String arg0, Scriptable arg1, Object arg2) {} // no
	public void put(int arg0, Scriptable arg1, Object arg2) {} // no
	public void setParentScope(Scriptable s) {} // no
	public void setPrototype(Scriptable p) {} // no
}
