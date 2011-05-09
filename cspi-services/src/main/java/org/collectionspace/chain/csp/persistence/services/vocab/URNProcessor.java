/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;

public class URNProcessor {
	private String syntax;
	private Pattern pattern;
	private int[] order;

	public URNProcessor(String syntax) {
		this.syntax=syntax;
		final Map<Integer,Integer> original=new HashMap<Integer,Integer>();
		original.put(0,-2);
		original.put(1,syntax.indexOf("{vocab}"));
		original.put(2,syntax.indexOf("{vocab}")+1);
		original.put(3,syntax.indexOf("{entry}"));
		original.put(4,syntax.indexOf("{entry}")+1);
		original.put(5,syntax.indexOf("{display}"));
		Integer[] rev=new Integer[]{0,1,2,3,4,5};
		Arrays.sort(rev,new Comparator<Integer>() {
			public int compare(Integer i1,Integer i2) {
				return original.get(i1).compareTo(original.get(i2));
			}		
		});
		pattern=Pattern.compile("(.*?)/"+syntaxToPattern(syntax));
		order=new int[6];
		for(int i=0;i<order.length;i++)
			order[rev[i]]=i+1;

	}

	/* regexp-escapes non-literals and replace {foo} with (.*?) */
	private static String syntaxToPattern(String in) {
		in=in.replaceAll("([^A-Za-z0-9])","\\\\$1");
		in=in.replaceAll("\\\\\\{display\\\\\\}","(.*?)");
		in=in.replaceAll("\\\\\\{.*?\\\\\\}","(.*?)\\\\((.*?)\\\\)");
		return in;
	}

	public String constructURN(String vocab_type,String vocab_id,String entry_type,String entry_id,String display) throws UnderlyingStorageException, ConnectionException, ExistException {
		try {
			String out=syntax;
			out=out.replaceAll("\\{vocab\\}",vocab_type+"("+URLEncoder.encode(vocab_id,"UTF-8")+")");
			out=out.replaceAll("\\{entry\\}",entry_type+"("+URLEncoder.encode(entry_id,"UTF-8")+")");
			out=out.replaceAll("\\{display\\}",URLEncoder.encode(display,"UTF-8"));
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		}
	}
	
	public boolean validUrn(String urn,boolean prefix){

		if(!prefix)
			urn="/"+urn;
		Matcher m=pattern.matcher(urn);
		if(!m.matches()){
			return false;
		}
		return true;
	}

	public String[] deconstructURN(String urn,boolean prefix) throws ExistException, UnderlyingStorageException {	
		try {
			if(!prefix)
				urn="/"+urn;
			Matcher m=pattern.matcher(urn);
			if(!m.matches())
				throw new ExistException("Bad URN, does not exist" + urn);
			String[] out=new String[6];
			// 2,4,5 were URI encoded.
			for(int i=0;i<out.length;i++) {
				out[i]=m.group(order[i]);
				if(i==2 || i==4 || i==5)
					out[i]=URLDecoder.decode(out[i],"UTF-8");
			}
			if(!prefix)
				out[0]=null;
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		}
	}
}
