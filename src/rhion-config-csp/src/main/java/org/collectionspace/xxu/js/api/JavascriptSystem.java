package org.collectionspace.xxu.js.api;

public interface JavascriptSystem {
	public JavascriptScript createScript() throws JavascriptException;
	public JavascriptContext createContext() throws JavascriptException;
	public JavascriptLibrary createLibrary() throws JavascriptException;
}
