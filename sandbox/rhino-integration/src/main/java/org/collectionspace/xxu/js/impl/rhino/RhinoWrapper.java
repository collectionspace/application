package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

public class RhinoWrapper implements Scriptable {	
	private Object thing;
	private RhinoReflectionCacheElement rce;
	private Scriptable scope;
	private Context ctx;
	
	RhinoWrapper(Context ctx,Scriptable scope,Object in) throws JavascriptException {
		thing=in;
		this.ctx=ctx;
		this.scope=scope;
		rce=RhinoSystem.getReflection().get(in.getClass());
		if(!rce.is_annotated())
			throw new JavascriptException("Cannot return unannotated object to javascript for security reasons");
	}
		
	Object getThing() { return thing; }
	
	public void delete(String arg0) {} // No!
	public void delete(int arg0) {} // No!
	
	private Object getFieldValue(Field f) {
		try {
			return RhinoContext.staticWrapIfNeeded(ctx,scope,f.get(thing));
		} catch (IllegalArgumentException e) {
			return Scriptable.NOT_FOUND;
		} catch (IllegalAccessException e) {
			return Scriptable.NOT_FOUND;
		}
	}

	private void setFieldValue(Field f,Object value) {
		try {
			f.set(thing,RhinoContext.UnwrapIfNeeded(ctx,value));
		} catch (IllegalArgumentException e) {
			// TODO Log it
		} catch (IllegalAccessException e) {
			// TODO Log it
		}
	}
		
	private Function getMethod(Method m) {
		return new RhinoFunction(this,thing,m);
	}
		
	// XXX support lists, maps, arrays
	public Object get(String name, Scriptable start) {
		Field f=rce.getField(name);
		if(f!=null)
			return getFieldValue(f);	
		Method m=rce.getMethod(name);
		if(m!=null)
			return getMethod(m);
		return Scriptable.NOT_FOUND;
	}

	public Object get(int arg0, Scriptable arg1) { return Scriptable.NOT_FOUND; } // TODO

	public String getClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getDefaultValue(Class arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object[] getIds() {
		// TODO Auto-generated method stub
		return null;
	}

	public Scriptable getParentScope() { return scope; }

	public Scriptable getPrototype() {
		Scriptable out=ScriptableObject.getClassPrototype(scope,"Object");
		return out;
	}

	public boolean has(String arg0, Scriptable arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean has(int arg0, Scriptable arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasInstance(Scriptable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void put(String name,Scriptable start,Object value) {
		Field f=rce.getField(name);
		if(f!=null)
			setFieldValue(f,value);
		// Methods cannot be set
		// No!
	}

	public void put(int arg0, Scriptable arg1, Object arg2) { } // TODO

	public void setParentScope(Scriptable arg0) {
		// TODO Auto-generated method stub

	}

	public void setPrototype(Scriptable arg0) {
		// TODO Auto-generated method stub

	}
}
