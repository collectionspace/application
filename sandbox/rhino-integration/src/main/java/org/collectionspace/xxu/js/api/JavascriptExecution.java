package org.collectionspace.xxu.js.api;

public interface JavascriptExecution {
	public Object execute(Object[] args) throws JavascriptException;
	public JavascriptContext getContext();
}
