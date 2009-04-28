package org.collectionspace.xxu.test.main;

import java.util.*;

public class ValidationAnd implements Validation {
	List<Validation> list=new ArrayList<Validation>();
	private Field f;
	
	public boolean validate(String in) {
		for(Validation v : list)
			if(!v.validate(in))
				return false;
		return true;
	}
	
	public String dump() {
		StringBuffer out=new StringBuffer();
		out.append("    BEGIN Validation\n");
		for(Validation v : list)
			out.append(v.dump());
		out.append("    END   Validation\n");
		return out.toString();
	}

	public void addValidation(Validation in) { list.add(in); in.setField(f); }
	
	public void setField(Field in) {
		f=in;
		for(Validation v : list)
			v.setField(f);
	}
	public Field getField() { return f; }
}
