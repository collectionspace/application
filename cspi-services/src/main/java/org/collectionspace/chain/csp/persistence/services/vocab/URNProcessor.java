package org.collectionspace.chain.csp.persistence.services.vocab;

import java.io.UnsupportedEncodingException;
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
	
	URNProcessor(String syntax) {
		this.syntax=syntax;
		final Map<Integer,Integer> original=new HashMap<Integer,Integer>();
		original.put(0,-2);
		original.put(1,syntax.indexOf("\\{vocab\\}"));
		original.put(2,syntax.indexOf("\\{entry\\}"));
		original.put(3,syntax.indexOf("\\{display\\}"));
		Integer[] rev=new Integer[]{0,1,2,3};
		Arrays.sort(rev,new Comparator<Integer>() {
			public int compare(Integer i1,Integer i2) {
				return original.get(i1).compareTo(original.get(i2));
			}		
		});
		pattern=Pattern.compile("(.*?)/"+syntaxToPattern(syntax));
		order=new int[4];
		for(int i=0;i<4;i++)
			order[rev[i]]=i+1;
		
	}
	
	/* regexp-escapes non-literals and replace {foo} with (.*?) */
	private static String syntaxToPattern(String in) {
		in=in.replaceAll("([^A-Za-z0-9])","\\\\$1");
		in=in.replaceAll("\\\\\\{.*?\\\\\\}","(.*?)");
		return in;
	}
	
	String constructURN(String vocab_id,String entry_id,String display) throws UnderlyingStorageException, ConnectionException, ExistException {
		try {
			String out=syntax;
			out=out.replaceAll("\\{vocab\\}",URLEncoder.encode(vocab_id,"UTF-8"));
			out=out.replaceAll("\\{entry\\}",URLEncoder.encode(entry_id,"UTF-8"));
			out=out.replaceAll("\\{display\\}",URLEncoder.encode(display,"UTF-8"));
			return out;
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("UTF-8 not supported!?");
		}
	}
	
	String[] deconstructURN(String urn) throws ExistException {	
		Matcher m=pattern.matcher(urn);
		if(!m.matches())
			throw new ExistException("Bad URN, does not exist");
		return new String[]{m.group(order[0]),m.group(order[1]),m.group(order[2]),m.group(order[3])};
	}
}
