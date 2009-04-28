package org.collectionspace.xxu.test.lib.httpclient;

public class Response {
	private String data;
	private boolean success;
	
	Response(boolean success,String data) { this.success=success; this.data=data; }
	
	public boolean success() { return success; }
	public String getResponse() { return data; }
}
