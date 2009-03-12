package org.collectionspace.xxu.js.impl.rhino;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.xxu.js.api.JavascriptException;
import org.collectionspace.xxu.js.api.JavascriptExecution;
import org.collectionspace.xxu.js.api.JavascriptLibrary;
import org.collectionspace.xxu.js.api.JavascriptSystem;


public class RhinoLibrary implements JavascriptLibrary {
	private Map<String,Object> props=new HashMap<String,Object>();
	private JavascriptSystem system;
	
	RhinoLibrary(JavascriptSystem s) { system=s; }
	
	public void addJavaClass(String name, Object in) throws JavascriptException {
		props.put(name,in);
	}

	public void addJavascriptExecution(String name, JavascriptExecution je) throws JavascriptException {
		if(!(je instanceof JavascriptExecution))
			throw new JavascriptException("Must pass an instance of JavascriptExecution");
		props.put(name,((RhinoExecution)je).getFunction());
	}

	void register(RhinoRegistry r) {
		for(Map.Entry<String,Object> e : props.entrySet())
			r.register(e.getKey(),e.getValue());
	}
	
	public JavascriptSystem getSystem() { return system; }
}
