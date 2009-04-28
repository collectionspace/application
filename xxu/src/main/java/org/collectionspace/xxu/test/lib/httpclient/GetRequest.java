package org.collectionspace.xxu.test.lib.httpclient;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

public class GetRequest {
	private HttpClient client;
	private HttpMethod get;
	
	public GetRequest(String url) {
		client=new HttpClient();
		get=new GetMethod(url);
		
	}
	
	public Response go() {
		try {
			try {
				// XXX character sets
				client.executeMethod(get);
				return new Response(true,new String(get.getResponseBody(),"UTF-8"));
			} catch (Exception e) {
				return new Response(false,null);
			}
		} finally {
			get.releaseConnection();
		}
	}
}
