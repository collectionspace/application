/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;

// XXX synchronize me
public class IdGenerator implements Storage {
	private File base;
	private static final char[] letters=new char[]{'a','b','c','d','e','f','g','h','i','j',
		   'k','l','m','n','o','p','q','r','s','t',
		   'u','v','w','x','y','z'};
	private static final char[] digits=new char[]{'0','1','2','3','4','5','6','7','8','9'};
	
	private static final Map<String,String> patterns=new HashMap<String,String>();
	
	static {
		patterns.put("objects","OBJ2009.$");
		patterns.put("acquisition","ACQ2009.$");
		patterns.put("intake","IN2009.$");
		patterns.put("","MISC2009.$");
		patterns.put("test","@A@B@^^");
	}
	
	public IdGenerator(String store_root) {
		base=new File(store_root,"ids");
		if(!base.exists())
			base.mkdir();
	}

	private File getSequenceStore(String path) throws UnsupportedEncodingException {
		path=URLEncoder.encode(path.substring(3),"UTF-8");
		return new File(base,path);
	}

	// TODO more complex sequences
	private int getSequence(String path) throws IOException {
		File seq=getSequenceStore(path);
		int v=0;
		if(seq.exists()) {
			InputStream sn=new FileInputStream(seq);
			try {
				v=Integer.parseInt(IOUtils.toString(sn));
			} catch(NumberFormatException x) {}
			sn.close();
		}
		v++;
		OutputStream sn2=new FileOutputStream(seq);
		IOUtils.write(Integer.toString(v),sn2);
		sn2.close();
		return v;
	}
	
	private String sequenceToString(String path,int v) { // XXX temporary implementation
		path=path.substring(3);
		String pattern=patterns.get(path);
		if(pattern==null)
			pattern=patterns.get("");
		StringBuffer out=new StringBuffer();
		for(int i=pattern.length()-1;i>=0;i--) {
			char c=pattern.charAt(i);
			switch(c) {
			case '%':
			case '^':
				c=digits[v%digits.length];
				v/=digits.length;
				break;
			case '@':
				c=letters[v%letters.length];
				v/=letters.length;
				break;
			case '$':
				c='\0';
				out.insert(0,Integer.toString(v));
				v=0;
				break;
			}
			if(c!='\0')
				out.insert(0,c);
		}
		return out.toString();
	}
	
	public JSONObject getPathsJSON(String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		// TODO
		return null;
	}
	
	public String[] getPaths(String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		// TODO
		return null;
	}

	public JSONObject retrieveJSON(String filePath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out=new JSONObject();
		try {
			out.put("next",sequenceToString(filePath,getSequence(filePath)));
		} catch (JSONException e) {
			throw new UnderlyingStorageException("JSONException serialisng value",e);
		} catch (IOException e) {
			throw new UnderlyingStorageException("IOException serialisng value",e);
		}
		return out;
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void deleteJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

	public void updateJSON(String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}
	public void transitionWorkflowJSON(String filePath, String workflowTransition) 
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Invalid method for ids");
	}

}
