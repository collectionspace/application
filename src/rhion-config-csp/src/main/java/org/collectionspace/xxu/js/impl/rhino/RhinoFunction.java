package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class RhinoFunction extends RhinoBaseFunction implements Function {
	private Object thing;
	private Method method;
	
	RhinoFunction(Scriptable scope,Object thing,Method m) { super(scope); this.thing=thing; method=m; }
	
	public Object call(Context context,Scriptable scope, Scriptable that,Object[] args) {
		try {
			Object[] params=new Object[args.length];
			for(int i=0;i<args.length;i++)
				params[i]=RhinoContext.UnwrapIfNeeded(context,args[i]);
			Object out=method.invoke(thing,params);
			return RhinoContext.staticWrapIfNeeded(context,scope,out);
		} catch (IllegalArgumentException e) {
			RhinoContext.static_log(context,"Cannot invoke function"+e.getLocalizedMessage());
			return null;
		} catch (IllegalAccessException e) {
			RhinoContext.static_log(context,"Cannot invoke function"+e.getLocalizedMessage());
			return null;
		} catch (InvocationTargetException e) {
			RhinoContext.static_log(context,"Cannot invoke function"+e.getLocalizedMessage());
			return null;
		}
	}
}
