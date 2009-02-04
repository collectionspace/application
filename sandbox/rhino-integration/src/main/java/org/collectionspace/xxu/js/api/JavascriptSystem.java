package org.collectionspace.xxu.js.api;

// XXX munge results
public interface JavascriptSystem {
	public JavascriptScript createScript() throws JavascriptException;
	public JavascriptContext createContext() throws JavascriptException;
	public JavascriptLibrary createLibrary() throws JavascriptException;
}
