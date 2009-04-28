package org.collectionspace.xxu.test.main;

import java.util.regex.*;

public class ValidationRegexp implements Validation {
	private Pattern p;
	private String p_orig;
	private Field f;
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("    BEGIN RegexpValidation\n");
		out.append("      rexexp="+p_orig+"\n");
		out.append("    END   RegexpValidation\n");
		return out.toString();
	}

	public void setPattern(String in) {
		p_orig=in;
		p=Pattern.compile(in);
	}
	
	public String getPattern() { return p_orig; }
	
	public boolean validate(String in) {
		return p.matcher(in).matches();
	}	
	
	public void setField(Field in) { f=in; }
	public Field getField() { return f; }
}
