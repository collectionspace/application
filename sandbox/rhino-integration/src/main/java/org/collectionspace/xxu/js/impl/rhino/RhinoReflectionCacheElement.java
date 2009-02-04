package org.collectionspace.xxu.js.impl.rhino;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptVisible;

public class RhinoReflectionCacheElement {
	private Map<String,Method> methods=new HashMap<String,Method>();
	private Map<String,Field> fields=new HashMap<String,Field>();	
	private boolean annotated=false;
	
	@SuppressWarnings("unchecked")
	RhinoReflectionCacheElement(Class klass) {
		annotated=klass.isAnnotationPresent(JavascriptVisible.class);
		for(Method m : klass.getMethods()) {
			if(!m.isAnnotationPresent(JavascriptVisible.class))
				continue;
			methods.put(m.getName(),m);
		}
		for(Field f : klass.getFields()) {
			if(!f.isAnnotationPresent(JavascriptVisible.class))
				continue;
			fields.put(f.getName(),f);			
		}
	}
	
	boolean is_annotated() { return annotated; }	
	Field getField(String name) { return fields.get(name); }
	Method getMethod(String name) { return methods.get(name); }
}
