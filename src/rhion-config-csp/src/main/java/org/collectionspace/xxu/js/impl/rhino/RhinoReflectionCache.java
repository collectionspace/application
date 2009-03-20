package org.collectionspace.xxu.js.impl.rhino;

import java.util.HashMap;
import java.util.Map;

public class RhinoReflectionCache {
	@SuppressWarnings("unchecked")
	private Map<Class,RhinoReflectionCacheElement> elements=new HashMap<Class,RhinoReflectionCacheElement>();
	
	@SuppressWarnings("unchecked")
	public RhinoReflectionCacheElement get(Class klass) {
		RhinoReflectionCacheElement element=elements.get(klass);
		if(element==null) {
			element=new RhinoReflectionCacheElement(klass);
			elements.put(klass,element);
		}
		return element;
	}
}
