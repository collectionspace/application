package org.collectionspace.xxu.test.main;

import org.mozilla.javascript.JavaScriptException;

public class SuggestScript implements Suggest {
	private Field f;
	private String method;
	
	public String[] suggest(String in, int num) {
		try {
			return ScriptStore.toStringArray(f.getCSpace().getScript().execute_function(method,new String[]{in}));
		} catch (JavaScriptException e) {
			return null;
		}
	}

	public void setField(Field in) { f=in; }
	public Field getField() { return f; }
	
	public void setMethod(String method) { this.method=method; }
	public String getMethod() { return method; }
}
