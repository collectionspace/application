/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;

/**
 * Session specific functionality in regards to the conversation between the UI and the App
 * @author csm22
 *
 */
public class WebUISession implements UISession {
	private boolean old=false;
	private String id;
	private Random rnd=new Random();
	private Map<String,Object> data=new HashMap<String,Object>();
	private WebUIRequest webUIRequest;
	
	private String randomSession() throws UIException {
		String sessionid=rnd.nextLong()+":"+rnd.nextLong()+":"+System.currentTimeMillis();
        
		byte[] defaultBytes = sessionid.getBytes();
		try{
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();
		            
			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<messageDigest.length;i++) {
				String hex = Integer.toHexString(0xFF & messageDigest[i]); 
				if(hex.length()==1)
				hexString.append('0');
				hexString.append(hex);
			}
			return hexString+"";
		} catch(NoSuchAlgorithmException nsae){
			throw new UIException("MD5 not supported",nsae);
		}
	}
	
	/**
	 * Initialize id as a random id
	 * @param umbrella
	 * @throws UIException
	 */
	public WebUISession(WebUIUmbrella umbrella, WebUIRequest request) throws UIException {
		this.webUIRequest = request;
		id=randomSession();
	}
	
	@Override
	public UIRequest getUIRequest() {
		return this.webUIRequest;
	}
	
	void setOld() { old=true; }
	boolean isOld() { return old; }
	String getID() { return id; }

	/**
	 * remove data from the session HashMap
	 */
	public void deleteValue(String key) {
		data.remove(key);
	}

	/** 
	 * retrieve data from the sessions HashMap
	 */
	public Object getValue(String key) {
		return data.get(key);
	}

	/**
	 * Set key value pair in the session HashMap
	 */
	public void setValue(String key, Object value) {
		data.put(key,value);
	}
}
