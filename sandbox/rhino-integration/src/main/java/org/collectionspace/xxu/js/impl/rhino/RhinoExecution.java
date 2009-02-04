package org.collectionspace.xxu.js.impl.rhino;

import org.collectionspace.xxu.js.api.JavascriptContext;
import org.collectionspace.xxu.js.api.JavascriptException;
import org.collectionspace.xxu.js.api.JavascriptExecution;
import org.mozilla.javascript.Function;

public class RhinoExecution implements JavascriptExecution {
	private Function function;
	private RhinoContext context;

	RhinoExecution(RhinoContext context,String name) throws JavascriptException {
		Object s=context.getScope().get(name,context.getScope());
		if(!(s instanceof Function))
			throw new JavascriptException(name+" is not a function");
		function=(Function)s;
		this.context=context;
	}
	
	// XXX convert args/results into sane java form
	public Object execute(Object[] args) throws JavascriptException {
		return function.call(context.getSystem().getContext(),context.getScope(),null,args);
	}

	Function getFunction() { return function; }
	public JavascriptContext getContext() { return context; }
}
