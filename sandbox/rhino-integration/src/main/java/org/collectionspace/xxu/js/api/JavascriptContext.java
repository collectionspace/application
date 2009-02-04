package org.collectionspace.xxu.js.api;

public interface JavascriptContext {
	public void addScript(JavascriptScript jss) throws JavascriptException;
	public void addLibrary(JavascriptLibrary library) throws JavascriptException;
	public JavascriptExecution createExecution(String function_name) throws JavascriptException;
	public JavascriptSystem getSystem();
}
