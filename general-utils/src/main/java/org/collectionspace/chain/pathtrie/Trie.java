/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.pathtrie;

public class Trie {
	private TrieNode root=new TrieNode();
	
	// XXX test tries
	
	public void addMethod(String[] path,int extra,TrieMethod method) {
		root.addMethod(path,0,extra,method);
	}
	
	public boolean call(String[] path,Object pay) throws Exception {
		return root.call(path,0,pay);
	}
}
