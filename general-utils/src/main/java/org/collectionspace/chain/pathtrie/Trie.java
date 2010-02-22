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
