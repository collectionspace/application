package org.collectionspace.xxu.js.impl.rhino;

import org.collectionspace.xxu.js.api.JavascriptContext;
import org.collectionspace.xxu.js.api.JavascriptException;
import org.collectionspace.xxu.js.api.JavascriptExecution;
import org.collectionspace.xxu.js.api.JavascriptMessages;
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
		
	public Object execute(Object[] args) throws JavascriptException {
		for(int i=0;i<args.length;i++)
			args[i]=RhinoContext.staticWrapIfNeeded(context.getSystem().getContext(),context.getScope(),args[i]);
		Object out=function.call(context.getSystem().getContext(),context.getScope(),null,args);
		return RhinoContext.UnwrapIfNeeded(context.getSystem().getContext(),out);
	}

	Function getFunction() { return function; }
	public JavascriptContext getContext() { return context; }
}
