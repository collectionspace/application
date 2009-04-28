package org.collectionspace.xxu.test.main;

public interface Field {
	public String getID();
	public String dump();
	
	public void addValidation(Validation in);
	public boolean validate(String in);
	public void addSuggest(Suggest s);
	public String[] suggest(String in,int num);
	public void setCSpace(CSpace in);
	public CSpace getCSpace();
}
