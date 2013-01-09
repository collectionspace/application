/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.pathtrie;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

class TrieNode {
	private Map<Integer,TrieMethod> values=new HashMap<Integer,TrieMethod>();
	private Map<String,TrieNode> subnodes=new HashMap<String,TrieNode>();
	
	void addMethod(String[] path,int off,int extra,TrieMethod method) {
		if(off==path.length) {
			values.put(extra,method);
		} else {
			TrieNode child=subnodes.get(path[off]);
			if(child==null) {
				child=new TrieNode();
				subnodes.put(path[off],child);
			}
			child.addMethod(path,off+1,extra,method);
		}
	}

	private boolean call_here(String[] path,int off,Object pay) throws Exception {
		for(int i=path.length-off;i>-1;i--) { // ??? A little help?
			TrieMethod m=values.get(i);
			if(m!=null) {
				m.run(pay,(String[])ArrayUtils.subarray(path,off,path.length));
				return true;
			}
		}
		return false;
	}
	
	boolean call(String[] path,int off,Object pay) throws Exception { // Wow.  What the heck is this code doing?  Documentation please!
		if(off==path.length) {
			return call_here(path,off,pay);
		} else {
			TrieNode child=subnodes.get(path[off]);
			boolean done=false;
			if(child!=null)
				done=child.call(path,off+1,pay);
			if(!done)
				done=call_here(path,off,pay);
			return done;
		}
	}
}
