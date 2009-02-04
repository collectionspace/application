package org.collectionspace.xxu.js.impl.rhino;

import org.collectionspace.xxu.js.api.JavascriptContext;
import org.collectionspace.xxu.js.api.JavascriptException;
import org.collectionspace.xxu.js.api.JavascriptExecution;
import org.collectionspace.xxu.js.api.JavascriptLibrary;
import org.collectionspace.xxu.js.api.JavascriptScript;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class RhinoContext implements JavascriptContext {
	private RhinoSystem system;
	private Scriptable scope;
	private RhinoRegistry registry;
	
	RhinoContext(RhinoSystem system) {
		this.system=system;
		registry=new RhinoRegistry(this);
		Scriptable top=system.getScope();
		scope=system.getContext().newObject(top);
		scope.setPrototype(top);
		scope.setParentScope(null);
		scope.put("sys",scope,registry);
	}
	
	public void addLibrary(JavascriptLibrary library) throws JavascriptException {
		if(!(library instanceof RhinoLibrary))
			throw new JavascriptException("Must pass an instance of RhinoLibrary");
		((RhinoLibrary)library).register(registry);
	}

	Scriptable getScope() { return scope; }
	public RhinoSystem getSystem() { return system; }
	
	public void addScript(JavascriptScript jss) throws JavascriptException {
		if(!(jss instanceof RhinoScript))
			throw new JavascriptException("Must pass an instance of RhinoScript");
		((RhinoScript)jss).getScript().exec(system.getContext(),scope);
	}

	public JavascriptExecution createExecution(String function_name) throws JavascriptException {
		return new RhinoExecution(this,function_name);
	}
	
	public Object wrapIfNeeded(Object in) {
		return staticWrapIfNeeded(getSystem().getContext(),getScope(),in);
	}

	// XXX support hints
	public static Object staticWrapIfNeeded(Context ctx,Scriptable scope,Object in) {
		if((in instanceof Scriptable) || (in instanceof String) ||
		   (in instanceof Number)     || (in instanceof Boolean))
			return in;
		WrapFactory wf=ctx.getWrapFactory();
		return wf.wrap(ctx,scope,in,null);
	}
	
	public static Object UnwrapIfNeeded(Context ctx,Object in) {
		if(in instanceof RhinoWrapper)
			in=((RhinoWrapper)in).getThing();
		return in;	
	}
}
