package org.collectionspace.csp.api.ui;

public interface UISession {
	public static final String USERID="session.cred.uid";
	public static final String PASSWORD="session.cred.password";
	
	public Object getValue(String key);
	public void setValue(String key,Object value);
	public void deleteValue(String key);
}
