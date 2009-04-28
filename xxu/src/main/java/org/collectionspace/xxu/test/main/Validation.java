package org.collectionspace.xxu.test.main;

public interface Validation {
	public boolean validate(String in);
	public String dump();
	
	public void setField(Field in);
	public Field getField();
}
