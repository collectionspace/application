package org.collectionspace.xxu.test.main;

import org.mozilla.javascript.JavaScriptException;

public class ValidationScript implements Validation {
	private String method;
	private Field f;
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("    BEGIN ScriptValidation\n");
		out.append("      method="+method+"\n");
		out.append("    END   ScriptValidation\n");
		return out.toString();
	}

	public boolean validate(String in) {
		try {
			return ScriptStore.toBoolean(f.getCSpace().getScript().execute_function(method,new String[]{in}));
		} catch (JavaScriptException e) {
			System.err.println(e.getMessage()+"\n");
			return false;
		}
	}

	public void setField(Field in) { f=in; }
	public Field getField() { return f; }
	
	public void setMethod(String method) { this.method=method; }
	public String getMethod() { return method; }
}
