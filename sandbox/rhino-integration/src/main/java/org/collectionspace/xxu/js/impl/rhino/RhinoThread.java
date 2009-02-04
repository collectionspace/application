package org.collectionspace.xxu.js.impl.rhino;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

public class RhinoThread {
	private Context context;
	private ScriptableObject top;
	private static String registry=RhinoThread.class.getPackage().getName()+".RhinoRegistry";
	
	RhinoThread(ContextFactory cf) {
		context = cf.enterContext();
		top = context.initStandardObjects();
		context.setClassShutter(new ClassShutter(){
			public boolean visibleToScripts(String arg) { 
				if("org.mozilla.javascript.EcmaError".equals(arg))
					return true;
				if("org.mozilla.javascript.EvaluatorException".equals(arg))
					return true;
				if(registry.equals(arg))
					return true;
				return false;
			}
		});
		context.setWrapFactory(new RhinoWrapFactory());
	}

	Context getContext() { return context; }
	ScriptableObject getScope() { return top; }
}
