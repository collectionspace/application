package org.collectionspace.xxu.test.main;

import org.apache.commons.lang.*;
import org.collectionspace.xxu.test.lib.InitLib;
import org.mozilla.javascript.*;

public class ScriptStore {
	private StringBuffer script_string=new StringBuffer();
	private Scriptable script=null;
	private ContextFactory cf=new ContextFactory();
	
	public void setScript(String in) { script_string.append(in); }
	public String getScript() { return script_string.toString(); }
	
	private Scriptable getScript(Context cx) throws JavaScriptException {
		if(script==null) {
			script=cx.initStandardObjects(null);
			InitLib.init(script);
			cx.evaluateString(script,script_string.toString(),"<cmd>",1,null);
		}
		return script;
	}
	
	private String makeCall(String name,String[] args) {
		StringBuffer out=new StringBuffer();
		out.append(name);
		out.append('(');
		boolean first=true;
		for(String arg : args) {
			if(!first)
				out.append(',');
			first=false;
			out.append('\'');
			out.append(StringEscapeUtils.escapeJavaScript(arg));
			out.append('\'');
		}
		out.append(')');
		return out.toString();
	}
	
	public Object execute_function(String name,String[] args) throws JavaScriptException {
		String js=makeCall(name,args);
		Context cx=cf.enterContext();
		try {
			Scriptable s=getScript(cx);
			return  cx.evaluateString(s,js,"<cmd>",1,null);
		} finally {
			Context.exit();
		}
	}
	
	public static boolean toBoolean(Object in) { return Context.toBoolean(in); }
	
	public static String[] toStringArray(Object in) {
		return (String[])Context.jsToJava(in,new String[0].getClass());
	}
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("  BEGIN Script\n");
		out.append(script_string);
		out.append("  END   Script\n");
		return out.toString();
	}
}
