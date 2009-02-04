package org.collectionspace.xxu.js.impl.rhino;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptContext;
import org.collectionspace.xxu.js.api.JavascriptLibrary;
import org.collectionspace.xxu.js.api.JavascriptScript;
import org.collectionspace.xxu.js.api.JavascriptSystem;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

public class RhinoSystem implements JavascriptSystem {
	private static ContextFactory cf=new ContextFactory();
	
	private static final ThreadLocal<RhinoThread> rhino_thread = 
        new ThreadLocal<RhinoThread>() {
            @Override protected RhinoThread initialValue() { 
            	return new RhinoThread(cf); 
            }
    };
    private Map<Scriptable,RhinoContext> scope_to_context=new HashMap<Scriptable,RhinoContext>();
	private static RhinoReflectionCache reflection=new RhinoReflectionCache();
    
	public RhinoSystem() {}

	public JavascriptContext createContext() {
		RhinoContext out=new RhinoContext(this);
		scope_to_context.put(out.getScope(),out);
		return out;
	}
	public JavascriptScript createScript() { return new RhinoScript(this); }

	Context getContext() { return rhino_thread.get().getContext(); }
	ScriptableObject getScope() { return rhino_thread.get().getScope(); }
	RhinoContext getContextFromScope(Scriptable scope) {
		while(scope!=null) {
			RhinoContext out=scope_to_context.get(scope);
			if(out!=null)
				return out;
			scope=scope.getParentScope();
		}
		System.err.println("EEK!");
		return null;
	}
	
	public static RhinoReflectionCache getReflection() { return reflection; }
	public JavascriptLibrary createLibrary() { return new RhinoLibrary(this); }
	public WrapFactory getWrapFactory() { return getContext().getWrapFactory(); }
}
