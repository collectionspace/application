package org.collectionspace.xxu.js.api;

public interface JavascriptLibrary {
	public void addJavaClass(String name,Object in) throws JavascriptException;
	public void addJavascriptExecution(String name,JavascriptExecution je) throws JavascriptException;
	public JavascriptSystem getSystem();
}
